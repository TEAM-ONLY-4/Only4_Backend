package com.ureca.only4_be.batch.scheduler;

import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill.BillSendStatus;
import com.ureca.only4_be.domain.member.Member;
import com.ureca.only4_be.domain.member.MemberGrade;
import com.ureca.only4_be.domain.member.MemberRepository;
import com.ureca.only4_be.domain.receipt.PaymentMethod;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@SpringBatchTest
@ActiveProfiles("local")
@EmbeddedKafka(partitions = 1, topics = {"email.send.request"})
class NotificationBatchSchedulerTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils; // 배치를 수동으로 실행해주는 도구

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Autowired
    @Qualifier("notificationJob")
    private Job notificationJob;

    private Consumer<String, String> consumer;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(notificationJob);
        billRepository.deleteAll();
        memberRepository.deleteAll();

        // 테스트용 데이터 생성 (DB에 넣기)
        // 오늘 날짜에 알림을 받아야 하는 멤버와 청구서 생성
        Member member = memberRepository.save(Member.builder()
                .name("쿠로미")
                .email("d@naver.com")
                .phoneNumber("010-3000-0000").address("서울")
                .memberGrade(MemberGrade.VIP).paymentOwnerName("a").paymentName("a").paymentNumber("1").paymentMethod(PaymentMethod.CARD)
                .notificationDayOfMonth((short)LocalDate.now().getDayOfMonth()) // 오늘 날짜와 일치시켜야 Reader가 읽음
                .build());

        billRepository.save(Bill.builder()
                .member(member)
                .billSendStatus(BillSendStatus.BEFORE_SENT) // 발송 전 상태
                .totalBilledAmount(BigDecimal.valueOf(50000))
                .paymentOwnerNameSnapshot("a").paymentNameSnapshot("a").paymentNumberSnapshot("1")
                .billingYearMonth(LocalDate.now().withDayOfMonth(1))
                .build());

        // 3. Kafka Consumer 설정
        String group = "testGroup-" + UUID.randomUUID();
        Map<String, Object> props = KafkaTestUtils.consumerProps(group, "true", embeddedKafkaBroker);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, String> cf = new DefaultKafkaConsumerFactory<>(
                props, new StringDeserializer(), new StringDeserializer());
        consumer = cf.createConsumer();
        embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "email.send.request");
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) consumer.close();
        billRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("배치 Job이 실행되면 DB 상태가 변경되고 Kafka 메시지가 전송되어야 한다")
    void testNotificationJob() throws Exception {
        // [Given] Job 파라미터 설정 (오늘 날짜)
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("requestDate", LocalDate.now().toString())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        // [When] 배치 Job 실행
        // jobLauncherTestUtils가 스프링 컨텍스트에 등록된 'notificationJob'을 찾아 실행함
        JobExecution jobExecution = jobLauncherTestUtils.launchJob(jobParameters);

        // [Then] 1. 배치 실행 결과 검증 -> COMPLETED 여야 함
        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // [Then] 2. DB 상태 업데이트 검증 -> SENT로 바꼈는지 확인
        Bill updatedBill = billRepository.findAll().get(0);
        assertThat(updatedBill.getBillSendStatus()).isEqualTo(BillSendStatus.SENT);

        // [Then] 3. Kafka 메시지 수신 검증 -> 메시지가 왔는지 확인
        ConsumerRecord<String, String> record = KafkaTestUtils.getSingleRecord(consumer, "email.send.request", Duration.ofSeconds(20));
        String messageBody = record.value();

        assertThat(messageBody).contains("\"memberId\":");
        assertThat(messageBody).contains("\"billId\":");
        assertThat(messageBody).contains(String.valueOf(updatedBill.getId()));

        System.out.println(">>> ✅ 통합 테스트 성공! 수신 메시지: " + messageBody);
    }
}