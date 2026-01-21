package com.ureca.only4_be.batch.jobs.settlement.partitioner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SettlementPartitioner implements Partitioner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Map<String, ExecutionContext> partition(int gridSize) {
        // 1. 전체 ID 범위 조회 (제일 작은 ID, 제일 큰 ID)
        Long minId = jdbcTemplate.queryForObject("SELECT MIN(id) FROM member", Long.class);
        Long maxId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM member", Long.class);

        Map<String, ExecutionContext> result = new HashMap<>();

        // 데이터가 없으면 빈 맵 반환 (배치 종료)
        if (minId == null || maxId == null) {
            log.warn("Target Data is Empty!");
            return result;
        }

        // 2. 한 파티션당 처리할 개수 계산
        // 예: (100 - 1) / 10 + 1 = 10개씩
        // 전체 100만 개, gridSize = 10 이라면 100,000
        long targetSize = (maxId - minId) / gridSize + 1;

        long start = minId;
        long end = start + targetSize - 1;

        // 3. 파티션 생성
        for (int i = 0; i < gridSize; i++) {
            ExecutionContext value = new ExecutionContext();

            if (end >= maxId) {
                end = maxId;
            }

            // ★ 각 스레드(Slave)에게 "너는 여기서부터 저기까지만 해!" 라고 지정
            value.putLong("minId", start);
            value.putLong("maxId", end);

            result.put("partition" + i, value);
            log.info("Partition[{}] range: {} ~ {}", i, start, end);

            if (start >= maxId) {
                break;
            }

            start += targetSize;
            end += targetSize;
        }

        return result;
    }
}