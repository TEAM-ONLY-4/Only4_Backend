package com.ureca.only4_be.domain.reservation_notification;

import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface ReservationNotificationRepository extends JpaRepository<ReservationNotification, Long> {

    // "예약됨(SCHEDULED)" 상태이면서 "발송 예정 시간이 현재보다 과거(또는 현재)"인 것 조회
    List<ReservationNotification> findByReservationStatusAndScheduledSendAtLessThanEqual(
            ReservationStatus reservationStatus,
            LocalDateTime now
    );
    List<ReservationNotification> findAllByOrderByScheduledSendAtDesc();
}
