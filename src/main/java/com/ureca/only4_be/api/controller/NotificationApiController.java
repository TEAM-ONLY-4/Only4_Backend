package com.ureca.only4_be.api.controller;

import com.ureca.only4_be.api.dto.notification.BatchJobResponse;
import com.ureca.only4_be.api.dto.notification.NotificationStatDto;
import com.ureca.only4_be.api.dto.notification.ReservationRequest;
import com.ureca.only4_be.api.dto.notification.ReservationResponse;
import com.ureca.only4_be.api.service.NotificationService;
import com.ureca.only4_be.api.service.ReservationNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin(origins = "http://localhost:3000")
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationApiController {

    private final NotificationService notificationService;
    private final ReservationNotificationService reservationNotificationService;

    @Operation(summary = "발송 현황 통계 조회")
    @GetMapping("/stats")
    public ResponseEntity<List<NotificationStatDto>> getNotificationStats(){
        return ResponseEntity.ok(notificationService.getNotificationStats());
    }

    @Operation(summary = "미발송 청구서 수동 발송 (즉시 실행)")
    @PostMapping("/manual-send")
    public ResponseEntity<BatchJobResponse> runManualBatch(
            @RequestParam(required = false) String date
    ) {
        BatchJobResponse response = notificationService.runManualBatch(date);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "예약 등록", description = "발송 예약을 생성합니다.")
    @PostMapping("/reservations")
    public ResponseEntity<Long> createReservation(@RequestBody ReservationRequest request) {
        return ResponseEntity.ok(reservationNotificationService.createReservation(request));
    }

    @Operation(summary = "예약 목록 조회", description = "등록된 모든 예약 내역을 조회합니다.")
    @GetMapping("/reservations")
    public ResponseEntity<List<ReservationResponse>> getReservations() {
        return ResponseEntity.ok(reservationNotificationService.getAllReservations());
    }

    @Operation(summary = "예약 수정 (재예약)", description = "기존 예약을 취소하고 새로운 예약을 생성합니다.")
    @PutMapping("/reservations/{id}")
    public ResponseEntity<Long> updateReservation(
            @PathVariable Long id,
            @RequestBody ReservationRequest request
    ) {
        return ResponseEntity.ok(reservationNotificationService.updateReservation(id, request));
    }

    @Operation(summary = "예약 취소", description = "대기 중인 예약을 취소합니다.")
    @DeleteMapping("/reservations/{id}")
    public ResponseEntity<Void> cancelReservation(@PathVariable Long id) {
        reservationNotificationService.cancelReservation(id);
        return ResponseEntity.ok().build();
    }
}
