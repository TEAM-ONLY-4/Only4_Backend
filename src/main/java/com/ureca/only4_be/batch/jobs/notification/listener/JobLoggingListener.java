package com.ureca.only4_be.batch.jobs.notification.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Slf4j
@Component
public class JobLoggingListener implements JobExecutionListener {

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("===================================================================");
        log.info("🚀 [JOB START] 청구서 발송 배치 시작");
        log.info("🔹 Job Name: {}", jobExecution.getJobInstance().getJobName());
        log.info("🔹 파라미터: {}", jobExecution.getJobParameters());
        log.info("===================================================================");
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getCreateTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        Duration duration = Duration.between(startTime, endTime != null ? endTime : LocalDateTime.now());

        log.info("===================================================================");
        log.info("🏁 [JOB END] 청구서 발송 배치 종료");
        log.info("🔹 소요 시간: {} ms", duration.toMillis());
        log.info("🔹 최종 상태: {}", jobExecution.getStatus());

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("🚨 배치가 실패했습니다! 에러 내용을 확인하세요.");
        }
        log.info("===================================================================");
    }
}