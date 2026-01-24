package com.ureca.only4_be.admin.service;

import com.ureca.only4_be.admin.dto.settlement.SettlementStatusDto;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.member.MemberRepository;
import com.ureca.only4_be.global.exception.BusinessException;
import com.ureca.only4_be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.configuration.JobRegistry;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StopWatch;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class SettlementService {

    private final MemberRepository memberRepository;
    private final BillRepository billRepository;
    private final JobLauncher jobLauncher;
    private final JobRegistry jobRegistry;
    private final JobExplorer jobExplorer;

    // 정산 현황 조회
    @Transactional(readOnly = true)
    public SettlementStatusDto getSettlementStatus(String dateStr) {
        // 1. 날짜 파싱
        YearMonth ym;
        try {
            ym = (dateStr != null)
                    ? YearMonth.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM"))
                    : YearMonth.now();
        } catch (DateTimeParseException e) {
            // 파싱 에러가 나면 BusinessException을 던져서 GlobalExceptionHandler가 잡게 함
            // ErrorCode.INVALID_TYPE_VALUE (G003) 사용
            throw new BusinessException("날짜 형식이 올바르지 않습니다. (yyyy-MM 형식을 사용해주세요.)", ErrorCode.INVALID_TYPE_VALUE);
        }

        LocalDate targetDate = ym.atDay(1);

        // 2. 전체 회원 수
        long totalCount = memberRepository.count();

        // 3. 정산 완료 건수 (Bill 테이블 조회)
        long processedCount = billRepository.countByBillingYearMonth(targetDate);

        // 4. 총 청구 금액 (NULL 처리)
        BigDecimal totalAmount = billRepository.sumTotalAmountByMonth(targetDate);
        if (totalAmount == null) totalAmount = BigDecimal.ZERO;

        // 5. 마지막 배치 실행 시간 조회
        JobInstance lastJobInstance = jobExplorer.getLastJobInstance("settlementJob");
        JobExecution lastExecution = null;
        if (lastJobInstance != null) {
            lastExecution = jobExplorer.getLastJobExecution(lastJobInstance);
        }

        LocalDateTime lastExecutedAt = (lastExecution != null) ? lastExecution.getStartTime() : null;

        // 6. 완료 여부 판단
        boolean isCompleted = (totalCount > 0) && (processedCount >= totalCount);

        return SettlementStatusDto.builder()
                .targetDate(ym.toString())
                .totalTargetCount(totalCount)
                .processedCount(processedCount)
                .totalAmount(totalAmount)
                .lastExecutedAt(lastExecutedAt)
                .isCompleted(isCompleted)
                .build();
    }

    // 정산 배치 실행
    public String runSettlementJob(String dateStr) {
        String targetDate = (dateStr != null) ? dateStr : LocalDate.now().toString();

        // --------------------------------------------------------
        // [1. 사전 검증] 동기적으로 체크하여 즉시 409 에러 반환
        // --------------------------------------------------------

        // (1) 현재 실행 중인지 확인
        Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions("settlementJob");
        if (!runningExecutions.isEmpty()) {
            throw new BusinessException("현재 정산 배치가 실행 중입니다.", ErrorCode.CONFLICT);
        }

        // (2) 파라미터 생성 (time 제외 -> 중복 방지)
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("targetDate", targetDate)
//                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();

        // (3) 이미 완료된 정산인지 확인
        JobInstance lastInstance = jobExplorer.getJobInstance("settlementJob", jobParameters);
        if (lastInstance != null) {
            JobExecution lastExecution = jobExplorer.getLastJobExecution(lastInstance);
            if (lastExecution != null && lastExecution.getStatus() == BatchStatus.COMPLETED) {
                throw new BusinessException("이미 완료된 정산입니다. 재실행할 수 없습니다.", ErrorCode.CONFLICT);
            }
        }

        // --------------------------------------------------------
        // [2. 비동기 실행] 검증 통과 시 백그라운드에서 실행 & 시간 측정
        // --------------------------------------------------------
        try {
            Job job = jobRegistry.getJob("settlementJob");

            CompletableFuture.runAsync(() -> {
                StopWatch stopWatch = new StopWatch();
                stopWatch.start();
                try {
                    log.info(">>>> [Batch Start] 정산 배치 실행 (Target: {})", targetDate);

                    // 실제 배치 수행
                    JobExecution execution = jobLauncher.run(job, jobParameters);

                    stopWatch.stop();

                    long totalSeconds = (long) stopWatch.getTotalTimeSeconds();
                    long minutes = totalSeconds / 60;
                    long seconds = totalSeconds % 60;
                    String timeStr = (minutes > 0) ? String.format("%d분 %d초", minutes, seconds) : String.format("%d초", seconds);

                    log.info(">>>> [Batch End] 완료! 소요시간: {}, 상태: {}", timeStr, execution.getStatus());

                } catch (Exception e) {
                    log.error(">>>> [Batch Error] 배치 실행 중 에러 발생", e);
                }
            });

            // 3. 즉시 성공 메시지 반환
            return "이번 달 정산 배치가 시작되었습니다.";

        } catch (Exception e) {
            log.error("배치 요청 실패", e);
            throw new BusinessException("배치 실행 중 오류가 발생했습니다.", ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }
}