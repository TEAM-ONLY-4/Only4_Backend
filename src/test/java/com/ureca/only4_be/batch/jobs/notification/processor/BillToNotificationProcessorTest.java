package com.ureca.only4_be.batch.jobs.notification.processor;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class BillToNotificationProcessorTest {

    private final BillToNotificationProcessor processor = new BillToNotificationProcessor();

    @Test
    @DisplayName("Bill 엔티티가 NotificationRequest DTO로 정상 변환된다")
    void process_success() throws Exception {
        // given (테스트 데이터 준비)
        Member member = Member.builder()
                .id(1L)
                .email("encrypted_email")
                .phoneNumber("encrypted_phone")
                .build();

        Bill bill = Bill.builder()
                .id(100L)
                .member(member)
                .billingYearMonth(LocalDate.of(2026, 1, 1))
                .totalBilledAmount(new BigDecimal("50000"))
                .paymentOwnerNameSnapshot("김유레카")
                .build();

        // when (실행)
        NotificationRequest result = processor.process(bill);

        // then (검증)
        assertThat(result).isNotNull();
        assertThat(result.getBillId()).isEqualTo(100L);
        assertThat(result.getMemberId()).isEqualTo(1L);
        assertThat(result.getEncryptedEmail()).isEqualTo("encrypted_email");
        assertThat(result.getTotalAmount()).isEqualTo(new BigDecimal("50000"));
        assertThat(result.getBillingDate()).isEqualTo("2026-01-01");
        assertThat(result.getOwnerName()).isEqualTo("김유레카");
    }
}