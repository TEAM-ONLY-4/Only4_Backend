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

    /**
     * Job 1: 적재(Staging) 전용 Job
     */
    @Bean("notificationStagingJob")
    public Job notificationStagingJob() {
        return new JobBuilder("notificationStagingJob", jobRepository)
                .start(step1Staging)
                .build();
    }

    /**
     * Job 2: 전송(Publishing) 전용 Job
     */
    @Bean("notificationPublishingJob")
    public Job notificationPublishingJob() {
        return new JobBuilder("notificationPublishingJob", jobRepository)
                .start(step2Publishing)
                .build();
    }

//    @Bean("notificationFullJob")
//    public Job notificationJob() {
//        return new JobBuilder("notificationJob", jobRepository)
//                .start(step1Staging) // Step 시작
//                .next(step2Publishing)
////                .incrementer(new RunIdIncrementer()) //테스트시 코드
//                .build();
//    }
}
