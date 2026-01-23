package com.ureca.only4_be.batch.jobs.notification;

import com.ureca.only4_be.batch.jobs.notification.listener.NotificationSkipListener;
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
    private final Step step1Staging;
    private final Step step2Publishing;

    @Bean("notificationJob")
    public Job notificationJob() {
        return new JobBuilder("notificationJob", jobRepository)
                .start(step1Staging) // Step 시작
                .next(step2Publishing)
                .build();
    }
}
