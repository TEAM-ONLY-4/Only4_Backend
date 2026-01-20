package com.ureca.only4_be.batch.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class BatchScheduler {

    private final JobLauncher jobLauncher;
    private final Job settlementJob;

    /**
     * [스케줄링 패턴]
     * cron = "초 분 시 일 월 요일"
     * "0 0 4 * * *" -> 매일 새벽 4시 0분 0초에 실행
     * (테스트할 때는 "0/10 * * * * *" 로 바꿔서 10초마다 도는지 확인)
     */
    @Scheduled(cron = "0 0 0 1 * *")
    public void runSettlementJob() {
        try {
            log.info(">>>> [Scheduler] 정산 배치 스케줄러 실행 시작");

            // 1. Job Parameter 생성
            // targetDate: 오늘 날짜로 실행
            // time: 중복 실행 방지용 (이게 없으면 하루에 한 번만 실행됨 -> 테스트할 때 불편)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("targetDate", LocalDate.now().toString())
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            // 2. Job 실행
            jobLauncher.run(settlementJob, jobParameters);

            log.info(">>>> [Scheduler] 정산 배치 로직 수행 완료 (Job Finished)");
        } catch (Exception e) {
            log.error(">>>> [Scheduler] 정산 배치 실행 중 에러 발생", e);
        }
    }
}