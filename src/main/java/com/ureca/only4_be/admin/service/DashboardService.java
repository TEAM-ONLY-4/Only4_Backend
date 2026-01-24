package com.ureca.only4_be.admin.service;

import com.ureca.only4_be.admin.dto.DashboardStatsDto;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill_notification.BillChannel;
import com.ureca.only4_be.domain.bill_notification.BillNotificationRepository;
import com.ureca.only4_be.domain.bill_notification.PublishStatus;
import com.ureca.only4_be.domain.bill_notification.SendStatus;
import com.ureca.only4_be.domain.member.MemberRepository;
import com.ureca.only4_be.global.exception.BusinessException;
import com.ureca.only4_be.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private final MemberRepository memberRepository;
    private final BillRepository billRepository;
    private final BillNotificationRepository billNotificationRepository;

    public DashboardStatsDto getDashboardStats(String dateStr) {
        // 1. 날짜 파싱 (yyyy-MM -> LocalDate)
        LocalDate targetDate;
        try {
            YearMonth ym = (dateStr != null)
                    ? YearMonth.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM"))
                    : YearMonth.now();
            targetDate = ym.atDay(1); // 해당 월의 1일로 설정
        } catch (DateTimeParseException e) {
            throw new BusinessException("날짜 형식이 올바르지 않습니다. (yyyy-MM)", ErrorCode.INVALID_TYPE_VALUE);
        }

        // 2. 총 가입자 수 (Member 테이블 전체 카운트)
        long totalMemberCount = memberRepository.count();

        // 3. 이번 달 정산 완료 건수 (Bill 테이블에서 해당 월 카운트)
        long settlementCompletedCount = billRepository.countByBillingYearMonth(targetDate);

        // 4. 이번 달 발송 대기 건수 (알림 테이블에서 PublishStatus가 PENDING인 것)
        long sendWaitingCount = billNotificationRepository.countByTargetDateAndPublishStatus(
                targetDate, PublishStatus.PENDING
        );

        // 5. 이메일 발송 성공 건수
        long emailSuccessCount = billNotificationRepository.countByTargetDateAndChannelAndSendStatus(
                targetDate, BillChannel.EMAIL, SendStatus.SENT
        );

        // 6. SMS 대체 발송 성공 건수
        long smsSuccessCount = billNotificationRepository.countByTargetDateAndChannelAndSendStatus(
                targetDate, BillChannel.SMS, SendStatus.SENT
        );

        return DashboardStatsDto.builder()
                .totalMemberCount(totalMemberCount)
                .settlementCompletedCount(settlementCompletedCount)
                .sendWaitingCount(sendWaitingCount)
                .emailSuccessCount(emailSuccessCount)
                .smsSuccessCount(smsSuccessCount)
                .build();
    }
}