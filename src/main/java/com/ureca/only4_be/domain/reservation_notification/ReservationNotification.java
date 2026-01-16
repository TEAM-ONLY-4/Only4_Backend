package com.ureca.only4_be.domain.reservation_notification;

import com.ureca.only4_be.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Entity
@Table(name = "reservation_notification")
public class ReservationNotification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @Column(name = "target_billing_year_month", nullable = false)
    private LocalDate targetBillingYearMonth;

    @Column(name = "scheduled_send_at", nullable = false)
    private LocalDateTime scheduledSendAt;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "reservation_status", nullable = false, columnDefinition = "reservation_status_enum")
    private ReservationStatus reservationStatus;
}
