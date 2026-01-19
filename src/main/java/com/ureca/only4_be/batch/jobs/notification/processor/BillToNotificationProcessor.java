package com.ureca.only4_be.batch.jobs.notification.processor;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.Bill;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;


@Component
public class BillToNotificationProcessor implements ItemProcessor<Bill, NotificationRequest> {

    @Override
    public NotificationRequest process(Bill bill) throws Exception {

        return NotificationRequest.builder()
                .billId(bill.getId())
                .memberId(bill.getMember().getId())
                .build();
    }
}
