package com.ureca.only4_be.batch.jobs.settlement.dto;

import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member_device.MemberDevice;
import com.ureca.only4_be.domain.one_time_purchase.OneTimePurchase;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class SettlementSourceDto {
    private Member member;
    private List<MemberDevice> memberDevices; // 기기 할부 정보 (기기 스펙 포함됨)
    private List<SubscriptionDetailDto> subscriptionDetails; // 구독 관련 상세 묶음
    private List<OneTimePurchase> oneTimePurchases; // 일회성 결제
}