package com.ureca.only4_be.batch.jobs.notification.listener;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill_notification.BillNotification;
import com.ureca.only4_be.domain.bill_notification.BillNotificationRepository;
import com.ureca.only4_be.domain.bill_notification.PublishStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationStep2SkipListener implements SkipListener<BillNotification, NotificationRequest> {

    private final BillNotificationRepository billNotificationRepository;
    private final PlatformTransactionManager transactionManager;

    @Override
    public void onSkipInRead(Throwable t) {
        log.error(">>> 🚫 [Step2 Skip-Read] 읽기 중 오류 발생. 사유: {}", t.getMessage());
    }

    @Override
    public void onSkipInProcess(BillNotification item, Throwable t) {
        log.error(">>> 🚫 [Step2 Skip-Process] 변환 중 오류 발생. ID: {}, 사유: {}", item.getId(), t.getMessage());
        // 변환 실패 시 해당 알림 건을 FAILED로 처리
        updateStatusToFailed(item.getId());
    }

    @Override
    public void onSkipInWrite(NotificationRequest item, Throwable t) {
        log.error(">>> 🚫 [Step2 Skip-Write] 전송 중 오류 발생. ID: {}, 사유: {}", item.getNotificationId(), t.getMessage());
        // Kafka 전송 or DB 업데이트 실패 시 FAILED로 처리
        updateStatusToFailed(item.getNotificationId());
    }

    /**
     * 실패한 건의 상태를 'FAILED'로 변경하는 독립 트랜잭션 메서드
     */
    private void updateStatusToFailed(Long notificationId) {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        // 기존 배치의 트랜잭션(롤백 중일 수 있음)과 분리하여, 실패 상태 저장은 확실히 커밋하기 위함
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        try {
            transactionTemplate.execute(status -> {
                BillNotification notification = billNotificationRepository.findById(notificationId).orElse(null);

                if (notification != null) {
                    // 엔티티에 편의 메서드(changePublishStatus)가 있다고 가정, 없으면 Setter 사용
                    notification.changePublishStatus(PublishStatus.FAILED);
                    billNotificationRepository.save(notification);
                    log.info(">>> [DB Update] 알림 ID {} 상태를 FAILED로 변경 완료", notificationId);
                } else {
                    log.warn(">>> [DB Update] 알림 ID {}를 찾을 수 없습니다.", notificationId);
                }
                return null;
            });
        } catch (Exception e) {
            log.error(">>> [DB Update Error] 상태 변경 중 DB 오류 발생: {}", e.getMessage());
        }
    }
}
