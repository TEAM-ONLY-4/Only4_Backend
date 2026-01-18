package com.ureca.only4_be.batch.jobs.settlement.reader;

import com.ureca.only4_be.batch.jobs.settlement.dto.*;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member_device.*;
import com.ureca.only4_be.domain.one_time_purchase.*;
import com.ureca.only4_be.domain.product.*;
import com.ureca.only4_be.domain.subscription.*;
import com.ureca.only4_be.domain.subscription_discount.*;
import com.ureca.only4_be.domain.subscription_usage.*;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementItemReader implements ItemReader<SettlementSourceDto> {

    private final EntityManagerFactory entityManagerFactory;
    private final SubscriptionRepository subscriptionRepository;
    private final MemberDeviceRepository memberDeviceRepository;
    private final OneTimePurchaseRepository oneTimePurchaseRepository;
    private final SubscriptionUsageRepository usageRepository;
    private final SubscriptionDiscountRepository discountRepository;
    private final MobilePlanSpecRepository mobilePlanSpecRepository;
    private final AddonSpecRepository addonSpecRepository;

    private JpaPagingItemReader<Member> delegateReader;

    @Override
    public SettlementSourceDto read() throws Exception {
        // 1. [하청] 회원 페이징 Reader 초기화
        if (delegateReader == null) {
            delegateReader = new JpaPagingItemReaderBuilder<Member>()
                    .name("delegateMemberReader")
                    .entityManagerFactory(entityManagerFactory)
                    .pageSize(100)
                    .queryString("SELECT m FROM Member m WHERE m.status = 'ACTIVE'")
                    .build();
            delegateReader.afterPropertiesSet();
        }

        // 2. 회원 1명 읽기
        Member member = delegateReader.read();
        if (member == null) return null;

        // 3. [조회] 회원이 구독한 모든 상품 정보 수집
        // (1) 기기 할부 (Device 포함 Fetch Join)
        List<MemberDevice> devices = memberDeviceRepository.findAllByMember(member);

        // (2) 일회성 결제
        List<OneTimePurchase> oneTimePurchases = oneTimePurchaseRepository.findAllByMember(member);

        // (3) 구독 (Product 포함 Fetch Join)
        List<Subscription> subscriptions = subscriptionRepository.findAllByMemberWithProduct(member);

        // 4. [조립] 구독 상세 정보 매핑
        List<SubscriptionDetailDto> subDetails = new ArrayList<>();
        if (!subscriptions.isEmpty()) {
            // --- [1] 구독 목록에서 'Product'들만 쏙 뽑아내기 (중복 제거) ---
            List<Product> products = subscriptions.stream()
                    .map(Subscription::getProduct)
                    .distinct() // 똑같은 상품 2개 구독했을 수도 있으니 중복 제거
                    .toList();

            // --- [2] 스펙 정보 한 방 조회 (IN 쿼리) ---
            // "이 상품들에 해당하는 스펙 있으면 다 내놔!"
            List<MobilePlanSpec> mobileSpecs = mobilePlanSpecRepository.findAllByProductIn(products);
            List<AddonSpec> addonSpecs = addonSpecRepository.findAllByProductIn(products);

            // --- [3] Map으로 정리 (Key: 상품 ID) ---
            Map<Long, MobilePlanSpec> mobileSpecMap = mobileSpecs.stream()
                    .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

            Map<Long, AddonSpec> addonSpecMap = addonSpecs.stream()
                    .collect(Collectors.toMap(s -> s.getProduct().getId(), s -> s));

            // --- [4] 사용량, 할인 조회 및 매핑 (기존 로직) ---
            List<SubscriptionUsage> usages = usageRepository.findAllBySubscriptionIn(subscriptions);
            List<SubscriptionDiscount> discounts = discountRepository.findAllBySubscriptionIn(subscriptions);

            Map<Long, List<SubscriptionUsage>> usageMap = usages.stream()
                    .collect(Collectors.groupingBy(u -> u.getSubscription().getId()));
            Map<Long, List<SubscriptionDiscount>> discountMap = discounts.stream()
                    .collect(Collectors.groupingBy(d -> d.getSubscription().getId()));

            // --- [5] 최종 조립 ---
            for (Subscription sub : subscriptions) {
                Long productId = sub.getProduct().getId(); // 상품 ID

                // 맵에서 꺼내기 (없으면 알아서 null이 나옴)
                MobilePlanSpec myMobileSpec = mobileSpecMap.get(productId);
                AddonSpec myAddonSpec = addonSpecMap.get(productId);

                subDetails.add(new SubscriptionDetailDto(
                        sub,
                        usageMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        discountMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        myMobileSpec, // ★ 모바일 스펙 주입 (없으면 null)
                        myAddonSpec   // ★ 부가서비스 스펙 주입 (없으면 null)
                ));
            }
        }

        // 5. 최종 DTO 반환
        return new SettlementSourceDto(member, devices, subDetails, oneTimePurchases);
    }
}