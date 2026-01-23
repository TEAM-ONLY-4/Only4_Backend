package com.ureca.only4_be.admin.service;

import com.ureca.only4_be.admin.dto.settlement.SettlementStatusDto;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.member.MemberRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

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
        YearMonth ym = (dateStr != null)
                ? YearMonth.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM"))
                : YearMonth.now();
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

        LocalDateTime lastExecutedAt = (lastExecution != null) ? lastExecution.getEndTime() : null;

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
}