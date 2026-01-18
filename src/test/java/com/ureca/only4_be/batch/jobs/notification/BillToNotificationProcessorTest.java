package com.ureca.only4_be.batch.jobs.notification;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.batch.jobs.notification.processor.BillToNotificationProcessor;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.member.Member;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class BillToNotificationProcessorTest {

    private final BillToNotificationProcessor processor = new BillToNotificationProcessor();

    @Test
    @DisplayName("Bill 엔티티가 NotificationRequest DTO로 정상 변환된다")
    void process_success() throws Exception {
        // Given
        Member member = Member.builder().build();
        // ID는 Setter가 없으므로 리플렉션으로 강제 주입
        ReflectionTestUtils.setField(member, "id", 100L);

        Bill bill = Bill.builder()
                .member(member)
                .build();
        // Bill ID 강제 주입
        ReflectionTestUtils.setField(bill, "id", 1L);

        // When (로직 실행)
        NotificationRequest result = processor.process(bill);

        // Then (검증: 결과가 맞는지 확인)
        assertThat(result).isNotNull();
        assertThat(result.getBillId()).isEqualTo(1L);
        assertThat(result.getMemberId()).isEqualTo(100L);

        System.out.println("테스트 성공! 변환된 DTO: " + result);
    }
}