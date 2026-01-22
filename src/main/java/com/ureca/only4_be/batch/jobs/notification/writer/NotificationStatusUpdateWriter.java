package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill_notification.BillNotificationRepository;
import com.ureca.only4_be.domain.bill_notification.PublishStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStatusUpdateWriter implements ItemWriter<NotificationRequest> {

    private final BillNotificationRepository billNotificationRepository;

    @Override
    public void write(Chunk<? extends NotificationRequest> chunk) {

        List<Long> notificationIds = chunk.getItems().stream()
                .map(NotificationRequest::getNotificationId)
                .collect(Collectors.toList());

        //빈 리스트 체크 (방어 로직)
        if (notificationIds.isEmpty()) {
            return;
        }

        billNotificationRepository.updatePublishStatus(
                notificationIds,
                PublishStatus.PUBLISHED
        );

        // 5. 로그 기록
        log.info(">>> 💾 [NotificationWriter] 알림 발송 상태 업데이트 완료 (PUBLISHED): {} 건", notificationIds.size());
    }
}