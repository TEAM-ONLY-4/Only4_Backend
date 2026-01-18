package com.ureca.only4_be.batch.jobs.settlement.reader;

import com.ureca.only4_be.batch.jobs.settlement.dto.*;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member_device.*;
import com.ureca.only4_be.domain.one_time_purchase.*;
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

        // 3. [조회] 5대장 데이터 수집
        // (1) 기기 할부 (Device 포함 Fetch Join)
        List<MemberDevice> devices = memberDeviceRepository.findAllByMember(member);

        // (2) 일회성 결제
        List<OneTimePurchase> oneTimePurchases = oneTimePurchaseRepository.findAllByMember(member);

        // (3) 구독 (Product 포함 Fetch Join)
        List<Subscription> subscriptions = subscriptionRepository.findAllByMemberWithProduct(member);

        // 4. [조립] 구독 상세 정보 매핑
        List<SubscriptionDetailDto> subDetails = new ArrayList<>();
        if (!subscriptions.isEmpty()) {
            // 사용량 & 할인 (IN 절로 한 방 조회 + DiscountPolicy 포함 Fetch Join)
            List<SubscriptionUsage> usages = usageRepository.findAllBySubscriptionIn(subscriptions);
            List<SubscriptionDiscount> discounts = discountRepository.findAllBySubscriptionIn(subscriptions);

            // 메모리 그룹핑 (구독 ID별 분류)
            Map<Long, List<SubscriptionUsage>> usageMap = usages.stream()
                    .collect(Collectors.groupingBy(u -> u.getSubscription().getId()));
            Map<Long, List<SubscriptionDiscount>> discountMap = discounts.stream()
                    .collect(Collectors.groupingBy(d -> d.getSubscription().getId()));

            for (Subscription sub : subscriptions) {
                subDetails.add(new SubscriptionDetailDto(
                        sub,
                        usageMap.getOrDefault(sub.getId(), new ArrayList<>()),
                        discountMap.getOrDefault(sub.getId(), new ArrayList<>())
                ));
            }
        }

        // 5. 최종 DTO 반환
        return new SettlementSourceDto(member, devices, subDetails, oneTimePurchases);
    }
}