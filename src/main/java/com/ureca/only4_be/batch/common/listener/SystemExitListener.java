package com.ureca.only4_be.batch.common.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.boot.SpringApplication;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemExitListener implements JobExecutionListener {

    private final ApplicationContext context;

    // 로컬에서는 값 주입안해서 안 꺼지도록
    @Value("${SPRING_BATCH_JOB_EXIT_ON_COMPLETION:false}")
    private boolean exitOnCompletion;

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (exitOnCompletion) {
            log.info(">>>> [SystemExitListener] 배치 작업 완료. 시스템을 종료합니다. (Status: {})", jobExecution.getStatus());
            
            // 종료 코드 계산 (실패면 1, 성공이면 0)
            int exitCode = jobExecution.getStatus().isUnsuccessful() ? 1 : 0;
            
            // 안전하게 스프링 컨텍스트 닫고 JVM 종료
            System.exit(SpringApplication.exit(context, () -> exitCode));
        }
    }
}
