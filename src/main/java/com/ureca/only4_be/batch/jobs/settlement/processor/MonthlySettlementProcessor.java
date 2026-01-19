package com.ureca.only4_be.batch.jobs.settlement.processor;

import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.batch.jobs.settlement.dto.SettlementSourceDto;
import com.ureca.only4_be.batch.jobs.settlement.dto.SubscriptionDetailDto;
import com.ureca.only4_be.batch.jobs.settlement.common.ProductIdRange;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillSendStatus;
import com.ureca.only4_be.domain.bill_item.BillItem;
import com.ureca.only4_be.domain.bill_item.BillItemCategory;
import com.ureca.only4_be.domain.product.BillItemSubcategory;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member_device.MemberDevice;
import com.ureca.only4_be.domain.one_time_purchase.OneTimePurchase;
import com.ureca.only4_be.domain.product.AddonSpec;
import com.ureca.only4_be.domain.product.MobilePlanSpec;
import com.ureca.only4_be.domain.product.Product;
import com.ureca.only4_be.domain.product.TvSpec;
import com.ureca.only4_be.domain.subscription.Subscription;
import com.ureca.only4_be.domain.subscription_discount.SubscriptionDiscount;
import com.ureca.only4_be.domain.subscription_usage.SubscriptionUsage;
import com.ureca.only4_be.domain.subscription_usage.UsageType;
import com.ureca.only4_be.domain.discount_policy.DiscountMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Configuration
@StepScope
@RequiredArgsConstructor
public class MonthlySettlementProcessor implements ItemProcessor<SettlementSourceDto, BillResultDto> {

    // LTE 표준 요금제 ID 상수
    private static final Long LTE_STANDARD_PLAN_ID = 130L;

    // JobParameter에서 날짜 받아오기
    @Value("#{jobParameters['targetDate']}")
    private String targetDateStr;

    @Override
    public BillResultDto process(SettlementSourceDto item) throws Exception {
        // 받아온 문자열 날짜를 LocalDate로 변환
//        LocalDate targetBillingDate = (targetDateStr != null)
//                ? LocalDate.parse(targetDateStr)
//                : LocalDate.now(); // 혹은 기본값

        Member member = item.getMember();
        LocalDate targetBillingDate = LocalDate.of(2026, 1, 1); // 현재 하드코딩 된 청구 기준일

        List<BillItem> billItems = new ArrayList<>();

        // 금액 집계 변수
        BigDecimal totalUsageAmount = BigDecimal.ZERO; // 총 사용 금액 (할인 전)
        BigDecimal totalDiscountAmount = BigDecimal.ZERO; // 총 할인 금액

        // ------------------------------------------------------
        // 1. 기기값 계산 (할부)
        // ------------------------------------------------------
        for (MemberDevice md : item.getMemberDevices()) {
            // 할부 개월 1인 경우 -> pass (일시불은 청구 대상 아님)
            if (md.getInstallmentMonths() <= 1) continue;

            BigDecimal devicePrice = md.getDevice().getPrice();
            BigDecimal months = BigDecimal.valueOf(md.getInstallmentMonths());

            // 월 할부금 = 기기값 / 할부개월 (소수점 버림 처리)
            BigDecimal monthlyAmount = devicePrice.divide(months, 0, RoundingMode.DOWN);

            // [수정됨] 스냅샷이 있는 메소드 호출
            billItems.add(createBillItemWithSnapshot(
                    BillItemCategory.ONE_TIME_PURCHASE, // 기기값은 일회성 구매 성격이나 할부이므로 ONE_TIME_PURCHASE 분류
                    BillItemSubcategory.ETC,
                    md.getDevice().getName(),
                    monthlyAmount,
                    createSimpleSnapshot("기기", "단말기 할부금")
            ));
            totalUsageAmount = totalUsageAmount.add(monthlyAmount);
        }

        // ------------------------------------------------------
        // 2. 일회성 결제 (소액결제, 콘텐츠 이용료 등)
        // ------------------------------------------------------
        for (OneTimePurchase otp : item.getOneTimePurchases()) {

            // ChargeType에 따른 서브카테고리 매핑 로직
            BillItemSubcategory subCategory;

            switch (otp.getChargeType()) {
                case CONTENT_FEE:
                    subCategory = BillItemSubcategory.CONTENT_FEE;
                    break;
                case MICRO_PAYMENT:
                case APP_PURCHASE: // 앱 결제도 소액결제로 통합
                case ROAMING:      // 로밍도 소액결제로 통합
                case ETC:
                default:
                    subCategory = BillItemSubcategory.MICRO_PAYMENT;
                    break;
            }

            // 스냅샷이 있는 메소드 호출
            billItems.add(createBillItemWithSnapshot(
                    BillItemCategory.ONE_TIME_PURCHASE,
                    subCategory, // ★ 위에서 결정한 카테고리 사용
                    otp.getName(),
                    otp.getAmount(),
                    createSimpleSnapshot("결제", "일회성 결제")
            ));
            totalUsageAmount = totalUsageAmount.add(otp.getAmount());
        }

        // ------------------------------------------------------
        // 3. 구독료 및 과금 계산 (핵심 로직)
        // ------------------------------------------------------
        for (SubscriptionDetailDto detail : item.getSubscriptionDetails()) {
            Subscription sub = detail.getSubscription();
            Product product = sub.getProduct();
            Long productId = product.getId();
            BigDecimal basePrice = product.getPriceAmount(); // 월정액

            BillItemCategory category = BillItemCategory.SUBSCRIPTION;
            BillItemSubcategory subCategory = BillItemSubcategory.ETC;
            Map<String, Object> snapshot = new HashMap<>();

            // 3-1. 모바일 요금제 (103 ~ 132)
            if (detail.getMobilePlanSpec() != null) {
                MobilePlanSpec spec = detail.getMobilePlanSpec();
                category = BillItemCategory.SUBSCRIPTION;
                subCategory = BillItemSubcategory.PLAN;

                // 스냅샷 생성 (카테고리, 요금항목, 스펙 리스트)
                snapshot = createMobileSnapshot("모바일", "월정액", spec);

                // [과금 계산] LTE 표준 요금제(130) 초과 과금 로직
                BigDecimal overageCharge = calculateOverageCharge(productId, spec, detail.getUsages(), billItems, product.getName());
                totalUsageAmount = totalUsageAmount.add(overageCharge);
            }

            // 3-2. TV 요금제 (1 ~ 42)
            else if (detail.getTvSpec() != null) {
                TvSpec spec = detail.getTvSpec();
                category = BillItemCategory.SUBSCRIPTION;
                subCategory = BillItemSubcategory.TV;
                snapshot = createSimpleSnapshot("TV", "월정액");
            }

            // 3-3. 부가서비스
            else if (detail.getAddonSpec() != null) {
                AddonSpec spec = detail.getAddonSpec();
                subCategory = BillItemSubcategory.ADDON;

                // 모바일/TV 부가서비스 분류 (ProductIdRange 사용)
                String categoryName = "기타";
                if (ProductIdRange.isMobileAddon(productId)) {
                    category = BillItemCategory.SUBSCRIPTION;
                    categoryName = "부가서비스(모바일)";
                } else if (ProductIdRange.isTvAddon(productId)) {
                    categoryName = "부가서비스(TV)";
                }
                snapshot = createSimpleSnapshot(categoryName, "월정액");
            }

            // 스냅샷이 있는 메소드 호출
            billItems.add(createBillItemWithSnapshot(category, subCategory, product.getName(), basePrice, snapshot));
            // 기본 월정액 추가
            totalUsageAmount = totalUsageAmount.add(basePrice);

            // [할인 적용] 약정 할인 있는 경우
            for (SubscriptionDiscount discount : detail.getDiscounts()) {
                BigDecimal discountValue = BigDecimal.ZERO;

                // 할인 정책의 방식(Method)과 값(Value)을 가져옴
                DiscountMethod method = discount.getDiscountPolicy().getDiscountMethod();
                BigDecimal policyValue = discount.getDiscountPolicy().getDiscountValue();

                if (method == DiscountMethod.RATE) {
                    // [정률 할인] (예: basePrice * 0.25)
                    discountValue = basePrice.multiply(policyValue);
                } else {
                    // [정액 할인] (예: 5000원) - AMOUNT
                    discountValue = policyValue;
                }

                totalDiscountAmount = totalDiscountAmount.add(discountValue);

                // 스냅샷이 없는 기본 메소드 호출
                billItems.add(createBillItem(
                        BillItemCategory.DISCOUNT,
                        BillItemSubcategory.SELECTIVE_CONTRACT,
                        discount.getDiscountPolicy().getDiscountName(),
                        discountValue.negate() // 음수 처리
                ));
            }
        }

        // ------------------------------------------------------
        // 4. 최종 청구서 생성
        // ------------------------------------------------------

        // 미납금 랜덤 (0 ~ 10,000원)
        BigDecimal unpaidAmount = BigDecimal.valueOf(ThreadLocalRandom.current().nextInt(0, 10001));

        // 총 사용 금액 - 총 할인 금액 = 할인 적용 후 금액
        BigDecimal discountedAmount = totalUsageAmount.subtract(totalDiscountAmount);
        if (discountedAmount.compareTo(BigDecimal.ZERO) < 0) discountedAmount = BigDecimal.ZERO;

        // 부가가치세 (할인 다 한 금액의 10%)
        BigDecimal vat = discountedAmount.multiply(BigDecimal.valueOf(0.1));

        // 총 청구 금액 = 할인 적용 후 금액 + VAT + 미납금
        BigDecimal totalBilledAmount = discountedAmount.add(vat).add(unpaidAmount);

        // 총 할인 금액 재계산 (0 미만인 경우 0 처리)
        // 위에서 totalDiscountAmount를 이미 구했으므로 그것을 사용하되 0 처리만 함
        if (totalDiscountAmount.compareTo(BigDecimal.ZERO) < 0) totalDiscountAmount = BigDecimal.ZERO;

        Bill bill = Bill.builder()
                .member(member)
                .billingYearMonth(targetBillingDate)
                .totalAmount(totalUsageAmount)
                .vat(vat)
                .unpaidAmount(unpaidAmount)
                .totalDiscountAmount(totalDiscountAmount)
                .totalBilledAmount(totalBilledAmount)
                .dueDate(LocalDate.of(2026, 1, 31))
                .approvalExpectedDate(LocalDate.of(2026, 1, 31))
                .billSendStatus(BillSendStatus.BEFORE_SENT)
                .paymentOwnerNameSnapshot(member.getPaymentOwnerName())
                .paymentNameSnapshot(member.getPaymentName())
                .paymentNumberSnapshot(member.getPaymentNumber())
                .build();

        return new BillResultDto(bill, billItems);
    }

    // ------------------------------------------------------
    // Helper Methods
    // ------------------------------------------------------

    // 과금 계산 로직 (LTE 표준 요금제 특수 처리 등)
    private BigDecimal calculateOverageCharge(Long productId, MobilePlanSpec spec, List<SubscriptionUsage> usages, List<BillItem> billItems, String productName) {
        BigDecimal totalOverage = BigDecimal.ZERO;

        // product_id = 130 (LTE 표준) 인 경우만 계산
        if (!LTE_STANDARD_PLAN_ID.equals(productId)) return BigDecimal.ZERO;

        // 사용량 집계
        BigDecimal dataUsed = BigDecimal.ZERO; // KB
        BigDecimal voiceUsed = BigDecimal.ZERO; // 초
        BigDecimal smsUsed = BigDecimal.ZERO; // 건

        for (SubscriptionUsage usage : usages) {
            if (usage.getUsageType() == UsageType.DATA) {
                // KB로 변환 (1GB = 1024 * 1024 KB)
                dataUsed = dataUsed.add(usage.getQuantity().multiply(BigDecimal.valueOf(1024 * 1024)));
            } else if (usage.getUsageType() == UsageType.VOICE) {
                // 초(Second)로 변환
                voiceUsed = voiceUsed.add(usage.getQuantity().multiply(BigDecimal.valueOf(60)));
            } else if (usage.getUsageType() == UsageType.SMS) {
                smsUsed = smsUsed.add(usage.getQuantity());
            }
        }

        // 1. 데이터 과금 (1KB당 0.275원)
        // LTE 표준 데이터 제공량이 0이라고 가정하거나, 제공량 초과분을 계산해야 함.
        // 스펙상 data_gb가 -1(무제한)이 아니면 계산. LTE 표준은 보통 기본 제공량이 매우 적음.
        if (spec.getDataGb().compareTo(BigDecimal.valueOf(-1)) != 0) {
            // 기본 제공량(GB)을 KB로 변환하여 차감
            BigDecimal limitKb = spec.getDataGb().multiply(BigDecimal.valueOf(1024 * 1024));
            BigDecimal overData = dataUsed.subtract(limitKb);

            if (overData.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal charge = overData.multiply(BigDecimal.valueOf(0.275));
                totalOverage = totalOverage.add(charge);
                // 스냅샷 없는 메소드 사용
                billItems.add(createBillItem(BillItemCategory.OVER_USAGE, BillItemSubcategory.DATA_OVER, productName + " 데이터 초과", charge));
            }
        }

        // 2. 음성 과금 (1초당 1.98원)
        if (spec.getVoiceMinutes() != -1) {
            BigDecimal limitSec = BigDecimal.valueOf(spec.getVoiceMinutes() * 60);
            BigDecimal overVoice = voiceUsed.subtract(limitSec);

            if (overVoice.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal charge = overVoice.multiply(BigDecimal.valueOf(1.98));
                totalOverage = totalOverage.add(charge);
                // 스냅샷 없는 메소드 사용
                billItems.add(createBillItem(BillItemCategory.OVER_USAGE, BillItemSubcategory.VOICE_OVER, productName + " 음성 초과", charge));
            }
        }

        // 3. 문자 과금 (기본 50건 제공 초과시 건당 요금 - 요금 명시 없으므로 건당 22원 표준 요금 가정)
        // SMS count check
        int limitSms = spec.getSmsCount() == null ? 0 : spec.getSmsCount();
        if (limitSms != -1) {
            BigDecimal overSms = smsUsed.subtract(BigDecimal.valueOf(limitSms));
            if (overSms.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal charge = overSms.multiply(BigDecimal.valueOf(22));
                totalOverage = totalOverage.add(charge);
                // 스냅샷 없는 메소드 사용
                billItems.add(createBillItem(BillItemCategory.OVER_USAGE, BillItemSubcategory.SMS_OVER, productName + " 문자 초과", charge));
            }
        }

        return totalOverage;
    }

    // ★ 1. 스냅샷이 있는 버전
    private BillItem createBillItemWithSnapshot(BillItemCategory category, BillItemSubcategory subCategory, String name, BigDecimal amount, Map<String, Object> snapshot) {
        return BillItem.builder()
                .itemCategory(category)
                .itemSubcategory(subCategory)
                .itemName(name)
                .amount(amount)
                .detailSnapshot(snapshot)
                .build();
    }

    // ★ 2. 스냅샷이 없는 버전 (기본형)
    private BillItem createBillItem(BillItemCategory category, BillItemSubcategory subCategory, String name, BigDecimal amount) {
        return BillItem.builder()
                .itemCategory(category)
                .itemSubcategory(subCategory)
                .itemName(name)
                .amount(amount)
                .detailSnapshot(null) // 스냅샷 없음
                .build();
    }

    // 3. 모바일 요금제용 상세 스냅샷
    private Map<String, Object> createMobileSnapshot(String categoryName, String itemName, MobilePlanSpec spec) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("카테고리", categoryName);
        snapshot.put("요금항목", itemName);

        List<String> specList = new ArrayList<>();

        String dataStr = (spec.getDataGb().compareTo(BigDecimal.valueOf(-1)) == 0)
                ? "데이터: 무제한 (5G 속도)"
                : "데이터: " + spec.getDataGb() + "GB";
        specList.add(dataStr);

        String voiceStr = (spec.getVoiceMinutes() == -1)
                ? "음성통화: 무제한 (초당 1.98원)"
                : "음성통화: " + spec.getVoiceMinutes() + "분";
        specList.add(voiceStr);

        String smsStr = (spec.getSmsCount() != null && spec.getSmsCount() == -1)
                ? "문자메시지: 기본 제공"
                : "문자메시지: " + (spec.getSmsCount() == null ? 0 : spec.getSmsCount()) + "건";
        specList.add(smsStr);

        snapshot.put("상품 스펙", specList);
        return snapshot;
    }

    // 4. 일반 요금제/부가서비스용 간단 스냅샷
    private Map<String, Object> createSimpleSnapshot(String categoryName, String itemName) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("카테고리", categoryName);
        snapshot.put("요금항목", itemName);
        return snapshot;
    }
}