package com.ureca.only4_be.batch.jobs.notification;

import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill.BillSendStatus;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member.MemberGrade;
import com.ureca.only4_be.domain.member.MemberRepository;
import com.ureca.only4_be.domain.receipt.PaymentMethod;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("local")
class NotificationJobTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils; // Job 실행 도구

    @Autowired
    private Job notificationJob;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private BillRepository billRepository;

    @AfterEach
    void tearDown() {
        billRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("지정된 날짜(requestDate) 파라미터를 받아 배치가 실행된다")
    void job_execution_success() throws Exception {
        // -------------------------------------------------------
        // Given: "2026-05-15" 날짜를 타겟으로 테스트 데이터 준비
        String targetDateStr = "2026-05-15";
        int targetDay = 15; // 15일

        Member member = Member.builder()
                .name("날짜테스트유저")
                .email("batch@test.com")
                .phoneNumber("010-9999-8888")
                .address("판교")
                .memberGrade(MemberGrade.VIP)
                .paymentOwnerName("홍길동")
                .paymentName("현대카드")
                .paymentNumber("1111-2222")
                .paymentMethod(PaymentMethod.CARD)
                .notificationDayOfMonth((short) targetDay)
                .build();
        memberRepository.saveAndFlush(member);

        Bill bill = Bill.builder()
                .member(member)
                .billingYyyymm("202602")
                .billSendStatus(BillSendStatus.BEFORE_SENT)
                .paymentOwnerNameSnapshot("홍길동")
                .paymentNameSnapshot("현대카드")
                .paymentNumberSnapshot("1111-2222")
                .totalBilledAmount(BigDecimal.valueOf(75000))
                .build();
        billRepository.saveAndFlush(bill);

        // -------------------------------------------------------
        // When: Job 실행 (파라미터로 날짜 넘기기)
        jobLauncherTestUtils.setJob(notificationJob);

        JobParameters jobParameters = new JobParametersBuilder()
                .addString("requestDate", targetDateStr)
                .toJobParameters();

        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // -------------------------------------------------------
        // Then: 결과 검증
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // "===== [Writer] 쓰기 작업 시작..." 로그가 찍혔다면 성공입니다.
    }
}