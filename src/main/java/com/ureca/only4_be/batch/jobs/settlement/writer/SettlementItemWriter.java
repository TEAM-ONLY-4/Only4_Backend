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
        // 1. 청구서(Bill) 리스트 추출
        List<Bill> bills = new ArrayList<>();
        for (BillResultDto dto : chunk) {
            bills.add(dto.getBill());
        }

        // 2. Bill 일괄 저장 (Batch Insert)
        // PostgreSQL Sequence 전략 덕분에, 저장 후 객체에 ID가 즉시 채워짐!
        // ★ 여기서 쿼리가 1000방 나가는 게 아니라, 1방(또는 소수)으로 뭉쳐서 나감
        billRepository.saveAll(bills);

        // 3. 저장된 Bill과 BillItem 연결 및 수집
        List<BillItem> allItems = new ArrayList<>();

        for (BillResultDto dto : chunk) {
            // dto.getBill()은 위에서 saveAll() 되면서 ID가 채워진 상태 (영속성 컨텍스트 공유)
            Bill savedBill = dto.getBill();

            for (BillItem item : dto.getBillItems()) {
                // Processor에서 넘어온 Item은 껍데기만 있으므로, 저장된 Bill과 연결해줌
                item.setBill(savedBill);
                allItems.add(item);
            }
        }

        // 4. BillItem 일괄 저장 (Batch Insert)
        // 이것도 쿼리 1000방 -> 1방으로 처리됨
        if (!allItems.isEmpty()) {
            billItemRepository.saveAll(allItems);
        }

        log.info("[Writer] 청구서 {}건, 항목 {}건 저장 완료 (Batch 적용됨)", bills.size(), allItems.size());
    }
}