package com.ureca.only4_be.batch.jobs.notification.processor;

import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill_notification.*;
import com.ureca.only4_be.domain.bill_notification.BillChannel;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class StagingProcessor implements ItemProcessor<Bill, BillNotification> {

    @Override
    public BillNotification process(Bill bill) throws Exception {
        return BillNotification.builder()
                .member(bill.getMember())
                .bill(bill)
                .channel(BillChannel.EMAIL)
                .publishStatus(PublishStatus.PENDING)
                .sendStatus(SendStatus.PENDING)
                .build();
    }
}
