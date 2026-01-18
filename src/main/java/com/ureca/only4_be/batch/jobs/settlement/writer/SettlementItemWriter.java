package com.ureca.only4_be.batch.jobs.settlement.writer;

import com.ureca.only4_be.batch.jobs.settlement.dto.BillResultDto;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill_item.BillItem;
import com.ureca.only4_be.domain.bill_item.BillItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SettlementItemWriter implements ItemWriter<BillResultDto> {

    private final BillRepository billRepository;
    private final BillItemRepository billItemRepository;

    @Override
    @Transactional
    public void write(Chunk<? extends BillResultDto> chunk) throws Exception {
        for (BillResultDto dto : chunk) {
            Bill bill = dto.getBill();
            List<BillItem> items = dto.getBillItems();

            // 1. Bill 저장 (이때 PK인 id가 생성됨)
            Bill savedBill = billRepository.save(bill);

            // 2. BillItem에 저장된 Bill 연결 (FK 설정)
            List<BillItem> readyToSaveItems = new ArrayList<>();
            for (BillItem item : items) {
                // Processor에서 만든 BillItem은 bill 필드가 비어있으므로 연결해줘야 함
                BillItem connectedItem = BillItem.builder()
                        .bill(savedBill) // ★ FK 연결
                        .itemCategory(item.getItemCategory())
                        .itemSubcategory(item.getItemSubcategory())
                        .itemName(item.getItemName())
                        .amount(item.getAmount())
                        .detailSnapshot(item.getDetailSnapshot()) // JSON 데이터
                        .build();
                readyToSaveItems.add(connectedItem);
            }

            // 3. BillItem 일괄 저장
            billItemRepository.saveAll(readyToSaveItems);
        }
        log.info("[Writer] 청구서 {}건 저장 완료", chunk.size());
    }
}