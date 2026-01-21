package com.ureca.only4_be.batch.jobs.notification.processor;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill_notification.BillNotification;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;


@Component
public class NotificationPublishProcessor implements ItemProcessor<BillNotification, NotificationRequest> {

    @Override
    public NotificationRequest process(BillNotification billNotification) throws Exception {

        Bill bill = billNotification.getBill();

        return NotificationRequest.builder()
                .notificationId(billNotification.getId())
                .billId(bill.getId())
                .memberId(bill.getMember().getId())
                .build();
    }
}
