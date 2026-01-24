package com.ureca.only4_be.api.service;

import com.ureca.only4_be.api.dto.notification.ReservationRequest;
import com.ureca.only4_be.api.dto.notification.ReservationResponse;
import com.ureca.only4_be.domain.reservation_notification.ReservationNotification;
import com.ureca.only4_be.domain.reservation_notification.ReservationNotificationRepository;
import com.ureca.only4_be.domain.reservation_notification.ReservationStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


@Slf4j
@Service
@RequiredArgsConstructor
public class ReservationNotificationService {

    private final ReservationNotificationRepository reservationRepository;

    @Transactional
    public Long createReservation(ReservationRequest request) {

        LocalDateTime scheduledTime = request.getScheduledSendAt();

        if (scheduledTime.isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("발송 예정 시간은 미래여야 합니다.");
        }

        ReservationNotification reservation = ReservationNotification.builder()
                .targetBillingYearMonth(request.getTargetBillingYearMonth())
                .scheduledSendAt(scheduledTime)
                .reservationStatus(ReservationStatus.SCHEDULED)
                .build();

        return reservationRepository.save(reservation).getId();
    }

    @Transactional
    public Long updateReservation(Long id, ReservationRequest request) {
        // 기존 예약 조회
        ReservationNotification oldReservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        // 검증 (이미 처리된 건은 수정 불가)
        if (oldReservation.getReservationStatus() != ReservationStatus.SCHEDULED) {
            throw new IllegalStateException("대기 중(SCHEDULED)인 예약만 변경할 수 있습니다.");
        }

        // 기존 예약 취소 처리 (Soft Delete)
        oldReservation.changeStatus(ReservationStatus.CANCELLED);

        // 새로운 예약 생성 (Insert - 위에서 만든 create 메서드 재사용)
        return createReservation(request);
    }

    @Transactional
    public void cancelReservation(Long id) {
        ReservationNotification reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("예약 정보를 찾을 수 없습니다."));

        if (reservation.getReservationStatus() != ReservationStatus.SCHEDULED) {
            throw new IllegalStateException("대기 중인 예약만 취소할 수 있습니다.");
        }

        reservation.changeStatus(ReservationStatus.CANCELLED);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> getAllReservations() {
        return reservationRepository.findAllByOrderByScheduledSendAtDesc().stream()
                .map(ReservationResponse::from)
                .collect(Collectors.toList());
    }
}