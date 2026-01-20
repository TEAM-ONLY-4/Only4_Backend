package com.ureca.only4_be.batch.jobs.settlement.reader;

import com.ureca.only4_be.batch.jobs.settlement.dto.*;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member.MemberRepository;
import com.ureca.only4_be.domain.member_device.*;
import com.ureca.only4_be.domain.one_time_purchase.*;
import com.ureca.only4_be.domain.product.*;
import com.ureca.only4_be.domain.subscription.*;
import com.ureca.only4_be.domain.subscription_discount.*;
import com.ureca.only4_be.domain.subscription_usage.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;

import java.time.LocalDate;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
// ★ 변경 1: ItemReader -> ItemStreamReader (Lifecycle 관리 포함)
public class SettlementItemReader implements ItemStreamReader<SettlementSourceDto> {

    // ==========================================
    // 1. Repositories (데이터 조회 담당)
    // ==========================================
    private final MemberRepository memberRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final OneTimePurchaseRepository oneTimePurchaseRepository;

    private final SubscriptionUsageRepository usageRepository;
    private final SubscriptionDiscountRepository discountRepository;
    private final MobilePlanSpecRepository mobilePlanSpecRepository;
    private final AddonSpecRepository addonSpecRepository;
    private final TvSpecRepository tvPlanSpecRepository;

    // ==========================================
    // 2. 상태 관리 변수들
    // ==========================================
    @Value("#{jobParameters['targetDate']}")
    private String targetDateString; // 컨트롤러가 넘겨준 날짜

    // [Buffer] DB에서 떼온 1,000명분 데이터를 잠시 보관하는 창고
    private final Queue<SettlementSourceDto> buffer = new LinkedList<>();

    // [Paging] 현재 페이지 위치와 한 번에 가져올 크기(1,000개)
    private int currentPage = 0;
    private final int PAGE_SIZE = 1000;

    // [Test Limit] 테스트용 50개 제한 (운영 시 제거 또는 주석 처리)
    private int readCount = 0;
    private final int MAX_ITEM_COUNT = 100;

    // ==========================================
    // 3. Main Logic (read)
    // ==========================================
    @Override
    public SettlementSourceDto read() throws Exception {
        // [안전장치] 테스트용 50개 제한 확인
        if (readCount >= MAX_ITEM_COUNT) {
            log.info(">>>> [Reader] 테스트 제한({}개)에 도달하여 배치를 정상 종료합니다.", MAX_ITEM_COUNT);
            return null; // null 반환 시 배치 종료
        }

        // [A] 창고(Buffer)에 데이터가 있으면? -> 하나 꺼내서 리턴 (DB 접근 X, 초고속)
        if (!buffer.isEmpty()) {
            readCount++;
            return buffer.poll();
        }

        // [B] 창고가 비었으면? -> 도매상 모드 발동! DB에서 1,000개 떼오기
        fillBuffer();

        // [C] 채워왔는데도 없으면? -> 진짜 데이터가 바닥난 것 -> 종료
        if (!buffer.isEmpty()) {
            readCount++;
            return buffer.poll();
        }

        return null;
    }

    // ==========================================
    // 4. Bulk Fetch Logic (fillBuffer)
    // ==========================================
    private void fillBuffer() {
        LocalDate targetDate = (targetDateString != null)
                ? LocalDate.parse(targetDateString)
                : LocalDate.of(2026, 1, 5);

        log.info(">>>> [Reader] Bulk Fetch 시작 (Page: {}, Date: {})", currentPage, targetDate);

        // -------------------------------------------------------
        // Step 1. [회원 조회] (페이징)
        // 청구서가 '없는' 회원 1,000명만 딱 잘라서 가져옵니다.
        // -------------------------------------------------------
        Pageable pageable = PageRequest.of(currentPage, PAGE_SIZE);
        Slice<Member> memberSlice = memberRepository.findMembersWithoutBill(targetDate, pageable);
        List<Member> members = memberSlice.getContent();

        if (members.isEmpty()) {
            return; // 더 이상 처리할 회원이 없음
        }

        // -------------------------------------------------------
        // Step 2. [ID 추출]
        // 찾아온 1,000명의 ID만 뽑아서 리스트로 만듭니다. (IN 절에 쓰려고)
        // -------------------------------------------------------
        List<Long> memberIds = members.stream().map(Member::getId).toList();

        // -------------------------------------------------------
        // Step 3. [연관 데이터 Bulk Fetch] (IN 절 + JOIN FETCH)
        // 회원 1명마다 쿼리 날리는 게 아니라, 1,000명분을 '한 방'에 가져옵니다.
        // -------------------------------------------------------

        // 3-1. 기기 정보 (MemberDevice + Device)
        List<MemberDevice> allDevices = memberDeviceRepository.findAllByMemberIdIn(memberIds);

        // 3-2. 일회성 구매 정보
        List<OneTimePurchase> allOtps = oneTimePurchaseRepository.findAllByMemberIdIn(memberIds);

        // 3-3. 구독 정보 (Subscription + Product)
        List<Subscription> allSubs = subscriptionRepository.findAllByMemberIdIn(memberIds);
        // -------------------------------------------------------
        // Step 4. [메모리 분류 (Grouping)]
        // 가져온 1,000개 뭉텅이를 회원 ID를 키(Key)로 하는 Map으로 정리.
        // 이렇게 해야 나중에 O(1) 속도로 "내 꺼"를 찾을 수 있다.
        // -------------------------------------------------------
        Map<Long, List<MemberDevice>> deviceMap = allDevices.stream()
                .collect(Collectors.groupingBy(d -> d.getMember().getId()));

        Map<Long, List<OneTimePurchase>> otpMap = allOtps.stream()
                .collect(Collectors.groupingBy(o -> o.getMember().getId()));

        Map<Long, List<Subscription>> subMap = allSubs.stream()
                .collect(Collectors.groupingBy(s -> s.getMember().getId()));

        // -------------------------------------------------------
        // Step 5. [구독 상세 데이터 조회]
        // 구독 정보가 있는 경우에만 추가 정보(사용량, 할인 등)를 또 Bulk Fetch
        // -------------------------------------------------------
        List<Product> products = allSubs.stream().map(Subscription::getProduct).distinct().toList();

        // 사용량/할인 정보도 IN 절로 한 방에
        List<SubscriptionUsage> usages = usageRepository.findAllBySubscriptionIn(allSubs);
        List<SubscriptionDiscount> discounts = discountRepository.findAllBySubscriptionIn(allSubs);

        // 스펙 정보 조회 (데이터 존재 시에만)
        List<MobilePlanSpec> mobileSpecs = (!products.isEmpty()) ? mobilePlanSpecRepository.findAllByProductIn(products) : Collections.emptyList();
        List<AddonSpec> addonSpecs = (!products.isEmpty()) ? addonSpecRepository.findAllByProductIn(products) : Collections.emptyList();
        List<TvSpec> tvSpecs = (!products.isEmpty()) ? tvPlanSpecRepository.findAllByProductIn(products) : Collections.emptyList();

        // 조회된 스펙들도 Map으로 변환 (Product ID 기준)
        Map<Long, MobilePlanSpec> mobileMap = mobileSpecs.stream().collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
        Map<Long, AddonSpec> addonMap = addonSpecs.stream().collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
        Map<Long, TvSpec> tvMap = tvSpecs.stream().collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

        // 사용량/할인도 Subscription ID 기준 Map으로 변환
        Map<Long, List<SubscriptionUsage>> usageMap = usages.stream().collect(Collectors.groupingBy(u -> u.getSubscription().getId()));
        Map<Long, List<SubscriptionDiscount>> discountMap = discounts.stream().collect(Collectors.groupingBy(d -> d.getSubscription().getId()));

        // -------------------------------------------------------
        // Step 6. [최종 조립 (Assembly)]
        // 이제 재료가 다 준비됐으니, 회원 1명씩 도시락(DTO)을 싸서 창고에 넣는다.
        // -------------------------------------------------------
        for (Member member : members) {
            Long memberId = member.getId();

            // Map에서 내 데이터 찾기 (DB 조회 아님, 메모리 조회라 매우 빠름)
            List<MemberDevice> myDevices = deviceMap.getOrDefault(memberId, new ArrayList<>());
            List<OneTimePurchase> myOtps = otpMap.getOrDefault(memberId, new ArrayList<>());
            List<Subscription> mySubs = subMap.getOrDefault(memberId, new ArrayList<>());

            // 구독 상세 DTO 만들기
            List<SubscriptionDetailDto> subDetails = new ArrayList<>();
            for (Subscription sub : mySubs) {
                Long pid = sub.getProduct().getId();
                subDetails.add(new SubscriptionDetailDto(
                        sub,
                        usageMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        discountMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        mobileMap.get(pid),
                        addonMap.get(pid),
                        tvMap.get(pid)
                ));
            }

            // 최종 DTO 생성 후 버퍼에 적재
            buffer.add(new SettlementSourceDto(member, myDevices, subDetails, myOtps));
        }

        // 다음 1,000명을 위해 페이지 번호 증가
        currentPage++;
    }

    // ==========================================
    // 5. Lifecycle Methods
    // ==========================================
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // 배치가 시작될 때 변수 초기화
        readCount = 0;
        currentPage = 0;
        buffer.clear();
        log.info(">>>> [SettlementItemReader] 리소스 초기화 완료");
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // 상태 저장 필요 시 구현
    }

    @Override
    public void close() throws ItemStreamException {
        // 리소스 정리
        buffer.clear();
        log.info(">>>> [SettlementItemReader] 리소스 정리 완료");
    }
}
