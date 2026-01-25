package com.ureca.only4_be.api.service;

import com.ureca.only4_be.api.dto.notification.NotificationStatDto;
import org.springframework.batch.core.Job;
import com.ureca.only4_be.api.dto.notification.BatchJobResponse;
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

import com.ureca.only4_be.global.service.EcsTaskService;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import org.springframework.beans.factory.annotation.Value;
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final BillNotificationRepository billNotificationRepository;
    private final JobLauncher jobLauncher;
    private final EcsTaskService ecsTaskService;
    private final Environment env;

    // 배포된 notification-task
    @Value("${cloud.aws.ecs.task-definitions.notification:}")
    private String notificationTaskDefinition;


    @Qualifier("notificationJob")
    private final Job notificationJob;

    @Transactional(readOnly = true)
    public List<NotificationStatDto> getNotificationStats(){
        return billNotificationRepository.findMonthlyStats();
    }
    public BatchJobResponse runManualBatch(String dateStr){
        String targetDate = (dateStr !=null) ? dateStr : LocalDate.now().toString();

        // [Prod 환경 분기 처리]
        if (Arrays.asList(env.getActiveProfiles()).contains("prod")) {
            log.info(">>>> [Prod Env] 알림 발송 배치 ECS Task 요청");
            List<KeyValuePair> envVars = List.of(
                    KeyValuePair.builder().name("BILLING_DATE").value(targetDate).build(),
                    KeyValuePair.builder().name("run.id").value(String.valueOf(System.currentTimeMillis())).build()
            );
            // task-notification.json과 값 일치
            ecsTaskService.runTask(notificationTaskDefinition, "notification-container", envVars);

            return BatchJobResponse.builder()
                    .jobId(0L)
                    .status("ECS_STARTED")
                    .exitCode("UNKNOWN")
                    .startTime(java.time.LocalDateTime.now())
                    .build();
        }

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
