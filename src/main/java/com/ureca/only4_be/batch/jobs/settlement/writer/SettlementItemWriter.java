package com.ureca.only4_be.batch.jobs.settlement.writer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SettlementItemWriter implements ItemWriter<String> { // 나중엔 BillResultDto로 변경

    @Override
    public void write(Chunk<? extends String> chunk) throws Exception {
        log.info("[Writer] DB 저장 시작 (Chunk size: {})", chunk.size());

        for (String item : chunk) {
            log.info("[Writer] 저장하는 척: {}", item);
        }

        // [TODO] 실제 구현 시:
        // 1. BillRepository.save(bill); -> Bill 저장
        // 2. 저장된 Bill의 ID를 BillItem에 주입
        // 3. BillItemRepository.saveAll(items); -> 상세 내역 저장
    }
}