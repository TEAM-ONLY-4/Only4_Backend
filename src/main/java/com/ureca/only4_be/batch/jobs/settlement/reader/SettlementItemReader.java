package com.ureca.only4_be.batch.jobs.settlement.reader;

import com.ureca.only4_be.batch.jobs.settlement.dto.*;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member_device.*;
import com.ureca.only4_be.domain.one_time_purchase.*;
import com.ureca.only4_be.domain.product.*;
import com.ureca.only4_be.domain.subscription.*;
import com.ureca.only4_be.domain.subscription_discount.*;
import com.ureca.only4_be.domain.subscription_usage.*;
import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import java.time.LocalDate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
// ★ 변경 1: ItemReader -> ItemStreamReader (Lifecycle 관리 포함)
public class SettlementItemReader implements ItemStreamReader<SettlementSourceDto> {

    private final EntityManagerFactory entityManagerFactory;
    private final SubscriptionRepository subscriptionRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final OneTimePurchaseRepository oneTimePurchaseRepository;
    private final SubscriptionUsageRepository usageRepository;
    private final SubscriptionDiscountRepository discountRepository;
    private final MobilePlanSpecRepository mobilePlanSpecRepository;
    private final AddonSpecRepository addonSpecRepository;
    private final TvSpecRepository tvPlanSpecRepository;

    // 컨트롤러가 보낸 'targetDate'를 여기서 받아옴! (SpEL 문법)
    @Value("#{jobParameters['targetDate']}")
    private String targetDateString;

    private JpaPagingItemReader<Member> delegateReader;

    // ★ 변경 2: read() 내부가 아니라, 서버 켜질 때 미리 초기화 (@PostConstruct)
    @PostConstruct
    public void init() {
        // [중요] 필터링 할 기준 날짜 (DB에 저장된 청구서 날짜와 일치해야 함)
        LocalDate targetDate = LocalDate.of(2026, 1, 5); // 하드 코딩

        // 받아온 문자열 날짜를 LocalDate로 변환
        // 값이 없으면 기본값 설정하는 로직 추가
//        LocalDate targetDate = (targetDateString != null)
//                ? LocalDate.parse(targetDateString)
//                : LocalDate.of(2026, 1, 5);

        log.info(">>>> [Reader] 설정된 정산 기준일: {}", targetDate);

        this.delegateReader = new JpaPagingItemReaderBuilder<Member>()
                .name("delegateMemberReader")
                .entityManagerFactory(entityManagerFactory)
                .pageSize(100)

                // ★ [핵심 수정] "Bill 테이블에 내 ID와 저 날짜로 된 데이터가 '없는' 경우만 가져와라"
                .queryString("SELECT m FROM Member m " +
                        "WHERE NOT EXISTS (" +
                        "   SELECT 1 FROM Bill b " +
                        "   WHERE b.member = m " +
                        "   AND b.billingYearMonth = :targetDate" +
                        ")")

                // ★ [핵심 수정] 쿼리 내부의 :targetDate 에 실제 날짜값 전달
                .parameterValues(Map.of("targetDate", targetDate))

                .maxItemCount(50) // 테스트용 1000명 제한 필요시 주석 해제
                .build();

        try {
            this.delegateReader.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Delegate Reader 초기화 실패", e);
        }
    }

    @Override
    public SettlementSourceDto read() throws Exception {
        // 1. 회원 1명 읽기 (이제 delegateReader는 이미 준비된 상태)
        Member member = delegateReader.read();
        if (member == null) return null;

        // 2. 기기 조회 (Device 포함 Fetch Join)
        List<MemberDevice> devices = memberDeviceRepository.findAllByMember(member);

        // 3. 일회성 결제 조회
        List<OneTimePurchase> oneTimePurchases = oneTimePurchaseRepository.findAllByMember(member);

        // 4. 구독 조회 (Product 포함 Fetch Join)
        List<Subscription> subscriptions = subscriptionRepository.findAllByMemberWithProduct(member);

        // 5. [조립] 구독 상세 정보 매핑
        List<SubscriptionDetailDto> subDetails = new ArrayList<>();
        if (!subscriptions.isEmpty()) {

            // [1] 상품 목록 추출
            List<Product> products = subscriptions.stream()
                    .map(Subscription::getProduct)
                    .distinct()
                    .toList();

            // [2] 3가지 스펙 테이블 동시에 조회 (IN 쿼리)
            // DB에 데이터가 있는 것만 가져오므로 ID 범위를 여기서 알 필요 없음
            List<MobilePlanSpec> mobileSpecs = mobilePlanSpecRepository.findAllByProductIn(products);
            List<AddonSpec> addonSpecs = addonSpecRepository.findAllByProductIn(products);
            List<TvSpec> tvSpecs = tvPlanSpecRepository.findAllByProductIn(products);

            // [3] Map핑 (빠른 검색용)
            Map<Long, MobilePlanSpec> mobileMap = mobileSpecs.stream()
                    .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
            Map<Long, AddonSpec> addonMap = addonSpecs.stream()
                    .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));
            Map<Long, TvSpec> tvMap = tvSpecs.stream()
                    .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

            // [4] 사용량/할인 Map 만들기
            List<SubscriptionUsage> usages = usageRepository.findAllBySubscriptionIn(subscriptions);
            List<SubscriptionDiscount> discounts = discountRepository.findAllBySubscriptionIn(subscriptions);

            Map<Long, List<SubscriptionUsage>> usageMap = usages.stream()
                    .collect(Collectors.groupingBy(u -> u.getSubscription().getId()));
            Map<Long, List<SubscriptionDiscount>> discountMap = discounts.stream()
                    .collect(Collectors.groupingBy(d -> d.getSubscription().getId()));

            // [5] 조립
            for (Subscription sub : subscriptions) {
                Long pid = sub.getProduct().getId();

                subDetails.add(new SubscriptionDetailDto(
                        sub,
                        usageMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        discountMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        mobileMap.get(pid), // 모바일 스펙 (없으면 null)
                        addonMap.get(pid),  // 부가서비스 스펙 (없으면 null)
                        tvMap.get(pid)      // TV 스펙 (없으면 null)
                ));
            }
        }

        // 6. 최종 DTO 반환
        return new SettlementSourceDto(member, devices, subDetails, oneTimePurchases);
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        // 배치가 시작될 때 이 메소드가 호출
        // 이때 내부의 delegateReader도 같이 'open' 해줘야 DB 연결이 생성
        log.info(">>>> [SettlementItemReader] Open! 내부 Reader 초기화 시작");
        delegateReader.open(executionContext);
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        delegateReader.update(executionContext);
    }

    @Override
    public void close() throws ItemStreamException {
        log.info(">>>> [SettlementItemReader] Close! 내부 Reader 리소스 정리");
        delegateReader.close();
    }
}