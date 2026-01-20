package com.ureca.only4_be.batch.jobs.notification;

import com.ureca.only4_be.batch.jobs.notification.listener.JobLoggingListener;
import com.ureca.only4_be.batch.jobs.notification.listener.NotificationSkipListener;
import com.ureca.only4_be.batch.jobs.notification.validator.DateJobParameterValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class NotificationJobConfig {

    private final JobRepository jobRepository;
    private final Step notificationStep;

    @Bean
    public Job notificationJob(JobLoggingListener jobLoggingListener) {
        return new JobBuilder("notificationJob", jobRepository)
                .start(notificationStep) // Step 시작
                // 검증기 추가: requestDate가 올바른지 확인
                .validator(new DateJobParameterValidator())
                // 모니터링 리스너 등록
                .listener(jobLoggingListener)
                .build();
    }
}
