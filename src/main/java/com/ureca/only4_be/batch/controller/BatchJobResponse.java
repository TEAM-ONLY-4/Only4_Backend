package com.ureca.only4_be.batch.controller;
import lombok.Builder;
import lombok.Getter;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;

import java.time.Duration;
import java.time.LocalDateTime;

@Getter
@Builder
public class BatchJobResponse {
    private Long jobExecutionId;
    private String jobName;
    private String status;           // 배치 상태 (COMPLETED, FAILED 등)
    private String exitCode;         // 종료 코드
    private String exitDescription;  // 에러 메시지 등 상세 설명
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String duration;

    // JobExecution 객체를 받아서 DTO로 변환하는 정적 메서드
    public static BatchJobResponse from(JobExecution execution) {
        LocalDateTime start = execution.getStartTime();
        LocalDateTime end = execution.getEndTime();

        // 소요 시간 계산
        String durationStr = "N/A";
        if (start != null && end != null) {
            Duration duration = Duration.between(start, end);
            durationStr = String.format("%d분 %d초 %dms",
                    duration.toMinutes(),
                    duration.toSecondsPart(),
                    duration.toMillisPart());
        }

        return BatchJobResponse.builder()
                .jobExecutionId(execution.getId())
                .jobName(execution.getJobInstance().getJobName())
                .status(execution.getStatus().toString())
                .exitCode(execution.getExitStatus().getExitCode())
                .exitDescription(execution.getExitStatus().getExitDescription())
                .startTime(start)
                .endTime(end)
                .duration(durationStr)
                .build();
    }
}