package com.ureca.only4_be.api.dto.notification;

import lombok.Builder;
import lombok.Getter;
import org.springframework.batch.core.JobExecution;

import java.time.LocalDateTime;

@Getter
@Builder
public class BatchJobResponse {
    private Long jobId;
    private String status;
    private String exitCode;
    private LocalDateTime startTime;

    // JobExecution 객체를 DTO로 변환하는 정적 메서드
    public static BatchJobResponse from(JobExecution execution) {
        return BatchJobResponse.builder()
                .jobId(execution.getJobId())
                .status(execution.getStatus().toString())
                .exitCode(execution.getExitStatus().getExitCode())
                .startTime(execution.getStartTime())
                .build();
    }
}
