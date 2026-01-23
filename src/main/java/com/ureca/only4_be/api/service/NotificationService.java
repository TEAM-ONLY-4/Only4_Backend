package com.ureca.only4_be.api.service;

import com.ureca.only4_be.api.dto.NotificationStatDto;
import org.springframework.batch.core.Job;
import com.ureca.only4_be.api.dto.BatchJobResponse;
import com.ureca.only4_be.domain.bill_notification.BillNotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BillNotificationRepository billNotificationRepository;
    private final JobLauncher jobLauncher;


    @Qualifier("notificationJob")
    private final Job notificationJob;

    @Transactional(readOnly = true)
    public List<NotificationStatDto> getNotificationStats(){
        return billNotificationRepository.findMonthlyStats();
    }
    public BatchJobResponse runManualBatch(String dateStr){
        String targetDate = (dateStr !=null) ? dateStr : LocalDate.now().toString();

        try{
            log.info(">>> [Service] 수동 배치 실행 시작. 기준일: {}", targetDate);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("billingDate", targetDate)
                    .addLong("time", System.currentTimeMillis())
                    .addString("requestSource", "API_MANUAL")
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(notificationJob, jobParameters);

            return BatchJobResponse.from(execution);
        } catch (Exception e) {
            log.error(">>> [Service] 배치 실행 중 에러 발생", e);
            throw new RuntimeException("배치 실행 실패: " + e.getMessage());
        }
    }

}
