package com.ureca.only4_be.batch.jobs.notification.reader;

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
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest // 스프링 컨텍스트 로드 (DB 연결됨)
@SpringBatchTest // 배치 테스트 유틸 기능 활성화
@ActiveProfiles("local") // local 프로필(DB 설정) 사용
//@Transactional // 테스트 끝나면 데이터 자동 롤백 (DB 깔끔하게 유지)
class NotificationItemReaderTest {

    @Autowired
    private JpaPagingItemReader<Bill> reader;
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
    @DisplayName("금지 시간대인 유저는 조회되지 않아야 한다")
    void read_filter_test() throws Exception {
        // 1. 현재 시간 기준으로 "금지 시간대" 설정
        // (지금이 17시라면, 금지시간을 16시~18시로 설정 -> 조회 안 되어야 정상)
        LocalTime now = LocalTime.now();
        LocalTime start = now.minusHours(1);
        LocalTime end = now.plusHours(1);
        int today = LocalDate.now().getDayOfMonth();

        // 2. Member 생성 (금지 시간 설정함)
        Member member = Member.builder()
                .name("금지된유저")
                // ... (필수값 채우기)
                .email("ban@test.com").phoneNumber("010-0000-0000").address("서울")
                .memberGrade(MemberGrade.VIP).paymentOwnerName("a").paymentName("a").paymentNumber("1").paymentMethod(PaymentMethod.CARD)
                .notificationDayOfMonth((short) today)
                .doNotDisturbStartTime(start) // 시작 시간
                .doNotDisturbEndTime(end)     // 종료 시간 (지금 이 사이에 있음)
                .build();
        memberRepository.saveAndFlush(member);

        // Bill 생성
        Bill bill = Bill.builder()
                .member(member)
                .billingYearMonth(LocalDate.of(2026, 5, 1))
                .billSendStatus(BillSendStatus.BEFORE_SENT) // 미발송이지만
                .paymentOwnerNameSnapshot("a").paymentNameSnapshot("a").paymentNumberSnapshot("1")
                .build();
        billRepository.saveAndFlush(bill);

        // Reader 실행
        reader.open(new ExecutionContext());
        Bill result = reader.read(); // 읽어오기

        // 검증: 금지 시간이므로 아무것도 안 읽혀야 함 (null)
        assertThat(result).isNull();

        System.out.println(">>> 테스트 성공! 금지 시간대 유저는 필터링되었습니다.");
    }
}
