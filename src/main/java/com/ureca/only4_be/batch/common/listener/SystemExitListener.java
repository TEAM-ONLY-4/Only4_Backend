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
            log.info(">>>> [SystemExitListener] 배치 작업 완료. 시스템 종료를 예약합니다. (Status: {})", jobExecution.getStatus());
            
            int exitCode = jobExecution.getStatus().isUnsuccessful() ? 1 : 0;

            // 별도 스레드에서 지연 종료 (마지막 로그 출력 시간 확보)
            new Thread(() -> {
                try {
                    Thread.sleep(5000); // 5초 대기
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                log.info(">>>> [SystemExitListener] 시스템 종료 수행 (Exit Code: {})", exitCode);
                System.exit(SpringApplication.exit(context, () -> exitCode));
            }).start();
        }
    }
}
