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
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope // 스레드마다 별도의 Reader 객체를 생성하게 함
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

    // ★ Partitioner가 넣어준 값 주입받기
    @Value("#{stepExecutionContext['minId']}")
    private Long minId;

    @Value("#{stepExecutionContext['maxId']}")
    private Long maxId;

    private Long lastId = 0L; // 커서

    private static final String LAST_ID_KEY = "lastId";
    private int totalReadCount = 0; // 로깅용 혹은 제한용

    // 파싱된 기준 날짜 (open에서 초기화)
    private LocalDate targetDate;

    private final Queue<SettlementSourceDto> buffer = new LinkedList<>();
    private final int PAGE_SIZE = 1000;

    // ==========================================
    // 3. Main Logic (read)
    // ==========================================
    @Override
    public SettlementSourceDto read() throws Exception {
        if (!buffer.isEmpty()) {
            totalReadCount++;
            return buffer.poll();
        }

        fillBuffer();

        if (!buffer.isEmpty()) {
            totalReadCount++;
            return buffer.poll();
        }
        return null;
    }

    // ==========================================
    // 4. Bulk Fetch Logic (fillBuffer)
    // ==========================================
    private void fillBuffer() {
        // [초기화] 커서가 아직 세팅 안 됐다면(0), 할당받은 minId 바로 앞번호로 설정
        if (lastId == 0L) {
            lastId = minId - 1;
        }

        // 이미 내 구역 끝(maxId)까지 다 읽었으면 조회 중단
        if (lastId >= maxId) {
            return;
        }

        // targetDate를 인자로 전달
        Slice<Member> memberSlice = memberRepository.findMembersByCursorWithRangeAndFilter(
                lastId,
                maxId,
                targetDate, // 중복 방지용 날짜
                PageRequest.of(0, PAGE_SIZE)
        );

        List<Member> members = memberSlice.getContent();
        if (members.isEmpty()) return;

        // 커서 업데이트 (다음 조회 시작점)
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
        // 리소스 초기화
        buffer.clear();
        totalReadCount = 0;

        // 날짜 파싱을 여기서 수행
        this.targetDate = (targetDateString != null)
                ? LocalDate.parse(targetDateString).withDayOfMonth(1)
                : LocalDate.of(2026, 1, 1);

        // 상태 복원 (재시작 시)
        if (executionContext.containsKey(LAST_ID_KEY)) {
            this.lastId = executionContext.getLong(LAST_ID_KEY);
            log.info(">>>> [RESTART] 파티션(min: {}, max: {}) - ID {}부터 다시 시작", minId, maxId, lastId);
        } else {
            this.lastId = 0L; // 초기화 (실제 값은 fillBuffer에서 minId - 1로 설정됨)
            log.info(">>>> [START] 파티션(min: {}, max: {}) 시작", minId, maxId);
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
        log.info(">>>> [CLOSE] 파티션 종료 (구역: {} ~ {}, 처리 건수: {}건, LastId: {})",
                minId, maxId, totalReadCount, lastId);
    }
}
