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
                .name("테스터")
                .build();

        Bill bill = Bill.builder()
                .id(100L)
                .member(member)
                .build();

        // when (실행)
        NotificationRequest result = processor.process(bill);

        // then (검증)
        assertThat(result).isNotNull();
        assertThat(result.getBillId()).isEqualTo(100L);
        assertThat(result.getMemberId()).isEqualTo(1L);

        System.out.println(">>> ✅ 유닛 테스트 성공");
    }
}