package com.ureca.only4_be.batch.jobs.settlement.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
// <입력타입, 출력타입> -> 지금은 String, String이지만 나중엔 <SettlementSourceDto, BillResultDto>로 변경
public class MonthlySettlementProcessor implements ItemProcessor<String, String> {

    @Override
    public String process(String item) throws Exception {
        log.info("[Processor] 데이터 처리 중... : {}", item);

        // [TODO] 실제 구현 시:
        // 1. SettlementSourceDto에서 구독 정보 꺼내기
        // 2. 할인 정책, 상품 가격 계산
        // 3. Bill(청구서) 및 BillItem(상세내역) 객체 생성 (DTO에 담기)

        return item + "_처리완료"; // Writer로 넘겨줄 값
    }
}