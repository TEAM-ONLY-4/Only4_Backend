package com.ureca.only4_be.batch.scheduler;

import com.ureca.only4_be.domain.reservation_notification.ReservationNotification;
import com.ureca.only4_be.domain.reservation_notification.ReservationNotificationRepository;
import com.ureca.only4_be.domain.reservation_notification.ReservationStatus;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;


import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class NotificationBatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job notificationJob;
    private final ReservationNotificationRepository reservationNotificationRepository;

    // 24시동작 3시간 간격
    @Scheduled(cron = "0 0 8/3 * * *")
    public void runRegularNotificationJob() {
        log.info(">>> [정기 스케줄러] 3시간 정기 배치 시작! 시간: {}", LocalDateTime.now());

        // 정기 발송은 '오늘 날짜' 기준으로 돕니다.
        String todayDate = LocalDate.now().toString();
        runBatch(todayDate, "REGULAR_BATCH");
    }

    @Scheduled(cron = "0 0 * * * *")
    public void runReservationJob() {
        LocalDateTime now = LocalDateTime.now();

        // 1. 지금 실행해야 할 예약이 있는지 확인
        List<ReservationNotification> targets = reservationNotificationRepository
                .findByReservationStatusAndScheduledSendAtLessThanEqual(ReservationStatus.SCHEDULED, now);

        if (targets.isEmpty()) {
            return; // 예약 없으면 조용히 종료 (로그도 안 남김)
        }

        log.info(">>> [예약 스케줄러] {} 건의 예약 발송을 시작합니다.", targets.size());

        for (ReservationNotification reservation : targets) {
            try {
                // 2. 예약 정보에 적힌 '타겟 청구월'로 배치 실행
                String targetDate = reservation.getTargetBillingYearMonth().toString();
                runBatch(targetDate, "RESERVATION_" + reservation.getId());

                // 3. 성공 시 상태 변경 (SCHEDULED -> DONE)
                reservation.changeStatus(ReservationStatus.DONE);
                log.info(">>> [예약 완료] ID: {}", reservation.getId());

            } catch (Exception e) {
                log.error(">>> [예약 실패] ID: {}", reservation.getId(), e);
                // 실패 시 상태 변경 (SCHEDULED -> FAILED)
                reservation.changeStatus(ReservationStatus.FAILED);
            }
        }
    }


        private void runBatch(String billingDate, String requestSource) {
            try {
                JobParameters jobParameters = new JobParametersBuilder()
                        .addString("billingDate", billingDate)
                        .addString("requestSource", requestSource) // 누가 실행했는지 추적용
                        .addLong("time", System.currentTimeMillis()) // 중복 실행 방지
                        .toJobParameters();

                JobExecution execution = jobLauncher.run(notificationJob, jobParameters);

                log.info(">>> ✅ 배치 실행 완료. Source: {}, Status: {}", requestSource, execution.getStatus());

            } catch (Exception e) {
                log.error(">>> 🚨 배치 실행 중 오류 발생. Source: {}", requestSource, e);
                // 예약 발송인 경우, 여기서 예외를 던져야 위쪽(runReservationJob) catch 블록에서 FAILED 처리를 할 수 있음
                throw new RuntimeException(e);
        }
    }
}