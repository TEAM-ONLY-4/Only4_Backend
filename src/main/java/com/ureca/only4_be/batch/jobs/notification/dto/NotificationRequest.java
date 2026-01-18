package com.ureca.only4_be.batch.jobs.notification.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class NotificationRequest {

    private Long billId;
    private Long memberId;

    // 수신자 정보 (암호화된 상태)
    private String encryptedEmail;
    private String encryptedPhone;

    private String billingDate;
    private BigDecimal totalAmount;
    private String ownerName;
}
