package com.ureca.only4_be.batch.jobs.notification.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class NotificationRequest {

    Long notificationId;
    Long memberId;
    Long billId;
}
