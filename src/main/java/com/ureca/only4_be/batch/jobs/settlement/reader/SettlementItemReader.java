package com.ureca.only4_be.batch.jobs.settlement.reader;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

@Slf4j
@Configuration
public class SettlementItemReader implements ItemReader<String> { // 지금은 String, 나중에 Member로 변경

    // 테스트용 가짜 데이터 (회원 이름이라고 가정)
    private final Iterator<String> dataIterator;

    public SettlementItemReader() {
        // [TODO] 실제 구현 시: JPA ItemReader 등을 사용하여 DB에서 Member와 Subscription 등을 조회하는 코드로 변경
        // 예: return new JpaPagingItemReaderBuilder<Member>()....build();
        List<String> mockData = Arrays.asList("테스트회원_1", "테스트회원_2", "테스트회원_3");
        this.dataIterator = mockData.iterator();
    }

    @Override
    public String read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (dataIterator.hasNext()) {
            String item = dataIterator.next();
            log.info("[Reader] 읽은 데이터: {}", item);
            return item;
        } else {
            return null; // null을 반환하면 배치가 끝난 것으로 인식함
        }
    }
}