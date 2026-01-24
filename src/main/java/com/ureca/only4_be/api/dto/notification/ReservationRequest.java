package com.ureca.only4_be.api.dto.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationRequest {
    private LocalDate targetBillingYearMonth;
    private LocalDateTime scheduledSendAt;
}
