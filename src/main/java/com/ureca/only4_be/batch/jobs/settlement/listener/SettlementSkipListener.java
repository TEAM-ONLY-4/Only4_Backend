package com.ureca.only4_be.batch.jobs.settlement.listener;

import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.batch.jobs.settlement.dto.SettlementSourceDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SettlementSkipListener implements SkipListener<SettlementSourceDto, BillResultDto> {

    // =========================================================
    // 1. [Reader 단계 실패]
    // DB 연결이 끊기거나, SQL 문법이 틀려서 아예 데이터를 못 가져온 경우
    // =========================================================
    @Override
    public void onSkipInRead(Throwable t) {
        log.error(">>>> [SKIP - 읽기 실패] 아이템 없음. 원인: {}", t.getMessage());
    }

    // =========================================================
    // 2. [Processor 단계 실패]
    // Reader는 성공해서 데이터를 넘겨줬는데, 계산 로직 돌리다가(Null 등) 에러난 경우
    // =========================================================
    @Override
    public void onSkipInProcess(SettlementSourceDto item, Throwable t) {
        // 여기서는 item(회원 정보)을 꺼낼 수 있음
        Long memberId = item.getMember().getId();
        log.error(">>>> [SKIP - 처리 실패] 회원ID: {}, 원인: {}", memberId, t.getMessage());
    }

    // =========================================================
    // 3. [Writer 단계 실패]
    // 계산까지 다 잘 끝났는데, 최종 DB에 Insert 하다가 에러난 경우
    // =========================================================
    @Override
    public void onSkipInWrite(BillResultDto item, Throwable t) {
        Long memberId = item.getBill().getMember().getId();
        log.error(">>>> [SKIP - 쓰기 실패] 회원ID: {}, 원인: {}", memberId, t.getMessage());
    }
}