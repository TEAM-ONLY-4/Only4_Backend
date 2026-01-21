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

    // [Buffer] 데이터를 잠시 보관하는 큐
    private final Queue<SettlementSourceDto> buffer = new LinkedList<>();

    // ★ [Cursor] 마지막으로 읽은 ID (0부터 시작)
    private Long lastId = 0L;
    private static final String LAST_ID_KEY = "lastId"; // 세이브 파일에 적을 변수명

    // 한 번에 가져올 개수 (Chunk Size와 맞춰주는 게 좋음)
    private final int PAGE_SIZE = 1000;

    // ★ [테스트용 제한 설정]
    // 운영(Production) 배포 시에는 주석 처리
    private int totalReadCount = 0;
    private final int TEST_LIMIT = 10000;

    // ==========================================
    // 3. Main Logic (read)
    // ==========================================
    @Override
    public SettlementSourceDto read() throws Exception {

        // [테스트 제한 체크]
        if (totalReadCount >= TEST_LIMIT) {
            log.info(">>>> [Reader] 테스트 제한({}명)에 도달하여 배치를 종료합니다.", TEST_LIMIT);
            return null;
        }

        // [A] 창고(Buffer)에 데이터가 있으면? -> 하나 꺼내서 리턴 (DB 접근 X, 초고속)
        if (!buffer.isEmpty()) {
            totalReadCount++; // 읽은 개수 증가
            return buffer.poll();
        }

        // [B] 버퍼 비었으면 DB 조회 (Bulk Fetch)
        fillBuffer();

        // [C] 채워왔는데도 없으면? -> 진짜 데이터가 바닥난 것 -> 종료
        if (!buffer.isEmpty()) {
            totalReadCount++; // 읽은 개수 증가
            return buffer.poll();
        }

        return null;
    }

    // ==========================================
    // 4. Bulk Fetch Logic (fillBuffer)
    // ==========================================
    private void fillBuffer() {
        // 들어온 날짜가 1월 20일이든 5일이든, 무조건 "그 달의 1일"로 바꿈
        LocalDate parsedDate = (targetDateString != null)
                ? LocalDate.parse(targetDateString)
                : LocalDate.of(2026, 1, 1); // 기본값

        LocalDate targetDate = parsedDate.withDayOfMonth(1);

        // ★ 테스트 제한에 거의 도달했다면? (성능 최적화)
        // 예: 1990명 읽었고 2000명 제한인데, 굳이 1000개를 더 퍼올 필요 없음.
        // 남은 개수만큼만 가져오도록 PAGE_SIZE 조절
        int remaining = TEST_LIMIT - totalReadCount;
        int currentFetchSize = Math.min(PAGE_SIZE, remaining);

        if (currentFetchSize <= 0) return; // 이미 다 채웠으면 조회 X

        log.info(">>>> [Reader] 커서 조회 시작 (LastId: {}, Date: {})", lastId, targetDate);

        // -------------------------------------------------------
        // Step 1. [회원 조회]
        // -------------------------------------------------------

        // ★ 무조건 0페이지 (Offset을 안 씀)
        // Offset으로 건너뛰는 게 아니라, WHERE id > lastId 조건으로 건너뜀
        // PageRequest는 오직 "LIMIT 1000"의 역할만 함.
        Pageable pageable = PageRequest.of(0, PAGE_SIZE);

        // ★ 커서 쿼리 실행 (id > lastId)
        Slice<Member> memberSlice = memberRepository.findMembersByCursor(lastId, targetDate, pageable);
        List<Member> members = memberSlice.getContent();

        if (members.isEmpty()) {
            return; // 더 이상 처리할 회원이 없음
        }

        // ★ 커서 업데이트 (다음 조회를 위해)
        // 이번에 가져온 목록 중 "가장 마지막 회원의 ID"를 기억해둠.
        // 다음 턴에는 이 ID보다 큰 녀석들부터 가져오게 됨.
        lastId = members.get(members.size() - 1).getId();

        // -------------------------------------------------------
        // Step 2. [ID 추출]
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
    }

    // ==========================================
    // 5. Lifecycle Methods
    // ==========================================
    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // 1. 배치가 시작될 때 리소스 초기화
        buffer.clear();
        totalReadCount = 0; // 읽은 개수 초기화

        // 2. [LOAD] 저장된 기록이 있는지 확인
        if (executionContext.containsKey(LAST_ID_KEY)) {
            this.lastId = executionContext.getLong(LAST_ID_KEY);
            log.info(">>>> [RESTART] 지난번 중단 지점(ID: {})부터 다시 시작합니다.", lastId);
        } else {
            this.lastId = 0L;
            log.info(">>>> [START] 처음부터 시작합니다. (ID: 0)");
        }
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        // [SAVE] 청크가 끝날 때마다 현재까지 읽은 마지막 ID를 기록
        executionContext.putLong(LAST_ID_KEY, lastId);
        log.debug(">>>> [SAVE] 상태 저장: lastId={}", lastId);
    }

    @Override
    public void close() throws ItemStreamException {
        // 리소스 정리
        buffer.clear();
        log.info(">>>> [CLOSE] 리소스 정리 완료");
    }
}
