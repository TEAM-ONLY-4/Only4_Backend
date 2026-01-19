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
    private NotificationItemReader notificationItemReaderConfig;

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
    @DisplayName("금지 시간대인 유저는 조회되지 않아야 한다 (Skip)")
    void read_filter_do_not_disturb_test() throws Exception {
        // [Given]
        // 1. 현재 시간 기준으로 금지 시간대 설정
        LocalTime now = LocalTime.now();
        LocalTime start = now.minusHours(1); // 1시간 전부터
        LocalTime end = now.plusHours(1);    // 1시간 후까지 (현재 포함됨)

        LocalDate today = LocalDate.now();

        // 2. Member 생성 (금지 시간 설정됨)
        Member member = Member.builder()
                .name("금지된유저")
                .email("ban@test.com").phoneNumber("010-0000-0000").address("서울")
                .memberGrade(MemberGrade.VIP).paymentOwnerName("a").paymentName("a").paymentNumber("1").paymentMethod(PaymentMethod.CARD)
                .notificationDayOfMonth((short) today.getDayOfMonth()) // 오늘 날짜에 받기로 함
                .doNotDisturbStartTime(start)
                .doNotDisturbEndTime(end)
                .build();
        memberRepository.save(member);

        // 3. Bill 생성 (이번 달 청구서)
        Bill bill = Bill.builder()
                .member(member)
                .billingYearMonth(today.withDayOfMonth(1)) // 이번 달 1일 (Reader 로직과 일치)
                .billSendStatus(BillSendStatus.BEFORE_SENT)
                .paymentOwnerNameSnapshot("a").paymentNameSnapshot("a").paymentNumberSnapshot("1")
                .totalBilledAmount(BigDecimal.valueOf(10000))
                .build();
        billRepository.save(bill);

        // [When] Reader 실행 (오늘 날짜 파라미터 전달)
        JpaPagingItemReader<Bill> reader = notificationItemReaderConfig.notificationReader(today.toString());
        reader.open(new ExecutionContext()); // 필수: Context 초기화

        // [Then] 금지 시간이므로 읽혀오지 않아야 함
        Bill result = reader.read();
        assertThat(result).isNull();

        System.out.println(">>> ✅ 테스트 성공! 금지 시간대 유저는 필터링되었습니다.");
    }

    @Test
    @DisplayName("Catch-up 테스트: 날짜가 지났어도 아직 못 받은 유저는 조회되어야 한다")
    void read_catch_up_test() throws Exception {
        // [Given]
        LocalDate today = LocalDate.now();
        int yesterday = today.minusDays(1).getDayOfMonth(); // 어제 날짜 (만약 오늘이 1일이면 로직 조정 필요하지만 테스트용이니 패스)

        // 1. Member 생성 (어제 받았어야 하는 유저)
        Member member = Member.builder()
                .name("지각유저")
                .email("late@test.com").phoneNumber("010-1111-1111").address("부산")
                .memberGrade(MemberGrade.NORMAL).paymentOwnerName("b").paymentName("b").paymentNumber("2").paymentMethod(PaymentMethod.CARD)
                .notificationDayOfMonth((short) yesterday) // 설정일이 어제임
                // 방해금지 시간 없음 (null)
                .build();
        memberRepository.save(member);

        // 2. Bill 생성 (상태: BEFORE_SENT or FAILED)
        Bill bill = Bill.builder()
                .member(member)
                .billingYearMonth(today.withDayOfMonth(1)) // 이번 달 청구서
                .billSendStatus(BillSendStatus.FAILED) // 이전에 실패했었다고 가정
                .paymentOwnerNameSnapshot("b").paymentNameSnapshot("b").paymentNumberSnapshot("2")
                .totalBilledAmount(BigDecimal.valueOf(20000))
                .build();
        billRepository.save(bill);

        // [When] Reader 실행 (기준일은 '오늘')
        // 쿼리 조건: notificationDay <= 오늘 (어제 날짜인 유저도 포함되어야 함)
        JpaPagingItemReader<Bill> reader = notificationItemReaderConfig.notificationReader(today.toString());
        reader.open(new ExecutionContext());

        // [Then] 조회되어야 함
        Bill result = reader.read();
        assertThat(result).isNotNull();
        assertThat(result.getMember().getName()).isEqualTo("지각유저");
        assertThat(result.getBillSendStatus()).isEqualTo(BillSendStatus.FAILED); // 실패했던 건도 가져옴

        System.out.println(">>> ✅ 테스트 성공! 지난 날짜(Catch-up) 유저 조회 성공.");
    }

    @Test
    @DisplayName("다른 달의 청구서는 조회되지 않아야 한다")
    void read_month_filter_test() throws Exception {
        // [Given]
        LocalDate today = LocalDate.now();
        LocalDate lastMonth = today.minusMonths(1).withDayOfMonth(1); // 지난달

        // Member (오늘 날짜 설정)
        Member member = Member.builder()
                .name("지난달유저")
                .email("past@test.com").phoneNumber("010-2222-2222").address("대구")
                .memberGrade(MemberGrade.NORMAL).paymentOwnerName("c").paymentName("c").paymentNumber("3").paymentMethod(PaymentMethod.CARD)
                .notificationDayOfMonth((short) today.getDayOfMonth())
                .build();
        memberRepository.save(member);

        // Bill (지난달 청구서 - 아직 안 보냈음)
        Bill bill = Bill.builder()
                .member(member)
                .billingYearMonth(lastMonth) // ★ 지난달 데이터
                .billSendStatus(BillSendStatus.BEFORE_SENT)
                .paymentOwnerNameSnapshot("c").paymentNameSnapshot("c").paymentNumberSnapshot("3")
                .totalBilledAmount(BigDecimal.valueOf(30000))
                .build();
        billRepository.save(bill);

        // [When] Reader 실행 (기준일: 오늘)
        // 쿼리 조건: billingYearMonth = 이번 달
        JpaPagingItemReader<Bill> reader = notificationItemReaderConfig.notificationReader(today.toString());
        reader.open(new ExecutionContext());

        // [Then] 지난달 데이터이므로 조회되지 않아야 함
        Bill result = reader.read();
        assertThat(result).isNull();

        System.out.println(">>> ✅ 테스트 성공! 지난 달 데이터 필터링 완료.");
    }
}
