package com.ureca.only4_be.batch.jobs.settlement.writer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill_item.BillItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementItemWriter implements ItemWriter<BillResultDto> {

    private final BillRepository billRepository; // 부모 저장은 JPA 사용
    private final JdbcTemplate jdbcTemplate;     // 자식 저장은 JDBC 사용
    private final ObjectMapper objectMapper; // ★ Map -> String 변환기 주입

    @Override
    @Transactional
    public void write(Chunk<? extends BillResultDto> chunk) throws Exception {

        // ==========================================
        // 1. [부모 저장] JPA 사용 (ID 획득용)
        // ==========================================
        List<Bill> bills = new ArrayList<>();
        for (BillResultDto dto : chunk) {
            bills.add(dto.getBill());
        }

        // JPA로 저장하면 bills 리스트 내부 객체들에 ID가 자동으로 채워짐
        billRepository.saveAll(bills);

        // ==========================================
        // 2. [평탄화 작업] (Flattening)
        // ==========================================
        List<BillItem> flatBillItems = new ArrayList<>();

        for (BillResultDto dto : chunk) {
            Bill savedBill = dto.getBill(); // ID가 채워진 Bill

            for (BillItem item : dto.getBillItems()) {
                // 부모의 ID를 자식에게 FK로 연결
                item.setBill(savedBill);
                flatBillItems.add(item);
            }
        }

        // ==========================================
        // 3. [자식 저장] JDBC Bulk Insert
        // ==========================================
        if (flatBillItems.isEmpty()) return;

        String sql = "INSERT INTO bill_item " +
                "(bill_id, item_category, item_subcategory, item_name, amount, detail_snapshot, status, created_date, modified_date) " +
                "VALUES (?, ?, ?, ?, ?, ?::jsonb, ?, NOW(), NOW())";

        jdbcTemplate.batchUpdate(sql, flatBillItems, flatBillItems.size(),
                (PreparedStatement ps, BillItem item) -> {
                    ps.setLong(1, item.getBill().getId());
                    ps.setString(2, item.getItemCategory().name());
                    ps.setString(3, item.getItemSubcategory() != null ? item.getItemSubcategory().name() : null);
                    ps.setString(4, item.getItemName());
                    ps.setBigDecimal(5, item.getAmount());
                    ps.setString(6, toJsonString(item.getDetailSnapshot()));
                    ps.setString(7, "ACTIVE");
                });

        log.info("[Writer] 청구서 {}건 (JPA), 항목 {}건 (JDBC) 저장 완료", bills.size(), flatBillItems.size());
    }

    // ★ 맵을 문자열로 바꾸는 헬퍼 메소드
    private String toJsonString(Map<String, Object> map) {
        if (map == null || map.isEmpty()) {
            return "{}"; // 빈 JSON 객체 반환
        }
        try {
            return objectMapper.writeValueAsString(map); // {"key":"value"} 형태로 변환
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 실패", e);
            return "{}"; // 에러 나면 빈 걸로 넣기
        }
    }
}