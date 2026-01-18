package com.ureca.only4_be.batch.jobs.settlement.dto;

import com.ureca.only4_be.domain.subscription.Subscription;
import com.ureca.only4_be.domain.subscription_discount.SubscriptionDiscount;
import com.ureca.only4_be.domain.subscription_usage.SubscriptionUsage;
import com.ureca.only4_be.domain.product.AddonSpec;       // import 추가
import com.ureca.only4_be.domain.product.MobilePlanSpec;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class SubscriptionDetailDto {
    private Subscription subscription;          // 구독 정보 (상품 정보 포함됨)
    private List<SubscriptionUsage> usages;     // 이 구독의 사용 내역
    private List<SubscriptionDiscount> discounts; // 이 구독의 할인 내역 (할인 정책 포함됨)

    // 스펙 정보들 (없으면 null 들어감)
    private MobilePlanSpec mobilePlanSpec;
    private AddonSpec addonSpec;
}