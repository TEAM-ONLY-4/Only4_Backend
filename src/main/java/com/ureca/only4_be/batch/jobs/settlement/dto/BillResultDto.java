package com.ureca.only4_be.batch.jobs.settlement.dto;

import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill_item.BillItem;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.util.List;

@Getter
@AllArgsConstructor
public class BillResultDto {
    private Bill bill;
    private List<BillItem> billItems;
}