package com.ureca.only4_be.api.dto.notification;
import com.ureca.only4_be.domain.reservation_notification.ReservationNotification;
import lombok.Builder;
import lombok.Getter;

import java.time.format.DateTimeFormatter;

@Getter
@Builder
public class ReservationResponse {
    private Long id;
    private String targetBillingYearMonth; // "2025-02"
    private String scheduledSendAt;        // "2026-01-24 14:00"
    private String status;                 // SCHEDULED, DONE 등

    public static ReservationResponse from(ReservationNotification entity) {
        return ReservationResponse.builder()
                .id(entity.getId())
                .targetBillingYearMonth(entity.getTargetBillingYearMonth().toString().substring(0, 7))
                .scheduledSendAt(entity.getScheduledSendAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .status(entity.getReservationStatus().name())
                .build();
    }

}
