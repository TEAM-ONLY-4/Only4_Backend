package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.domain.bill_notification.BillNotification;
import com.ureca.only4_be.domain.bill_notification.BillNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StagingWriter implements ItemWriter<BillNotification> {

    private final BillNotificationRepository billNotificationRepository;

    @Override
    public void write (Chunk<? extends BillNotification> chunk){
        billNotificationRepository.saveAll(chunk.getItems());
    }
}
