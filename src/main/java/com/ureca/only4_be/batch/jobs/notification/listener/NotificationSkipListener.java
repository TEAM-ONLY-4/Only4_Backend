package com.ureca.only4_be.batch.jobs.notification.listener;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.Bill;
import com.ureca.only4_be.domain.bill.BillRepository;
import com.ureca.only4_be.domain.bill.BillSendStatus;
import com.ureca.only4_be.domain.bill_notification.BillNotification;
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
public class NotificationSkipListener implements SkipListener<Bill, BillNotification> {

    private final BillRepository billRepository;
    private final PlatformTransactionManager transactionManager; // 트랜잭션 매니저 주입

    @Override
    public void onSkipInRead(Throwable t){
        log.error("읽기 중 요류 발생으로 건너뜀. 사유{}", t.getMessage());

    }
    @Override
    public void onSkipInProcess(Bill item, Throwable t){
        log.error("처리 중 요류 발생. Bill ID:{}, 사유:{}", item.getId(), t.getMessage());
        updateStatusToFail(item.getId());
    }
    @Override
    public void onSkipInWrite(BillNotification item, Throwable t){
        log.error("쓰기 중 오류 발생. Member ID:{}, 사유:{}", item.getBill().getId(), t.getMessage());
        updateStatusToFail(item.getBill().getId());
    }

    private void updateStatusToFail(Long billId) {
        // 1. 트랜잭션 템플릿 생성
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager);
        // 2. 전파 속성 설정: 기존 트랜잭션과 무관하게 항상 새로운 트랜잭션을 시작
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        // 3. 트랜잭션 실행 (이 블록 안의 내용은 무조건 커밋됨)
        try {
            transactionTemplate.execute(status -> {
                Bill bill = billRepository.findById(billId).orElse(null);

                if (bill != null) {
                    bill.changeStatus(BillSendStatus.FAILED); // 상태 변경
                    billRepository.save(bill); // 저장
                    log.info(">>> [DB Update] Bill ID {} 상태를 FAILED로 변경 완료", billId);
                } else {
                    log.warn(">>> [DB Update] Bill ID {}를 찾을 수 없습니다.", billId);
                }
                return null;
            });
        } catch (Exception e) {
            log.error(">>> [DB Update Error] 상태 변경 중 DB 오류 발생: {}", e.getMessage());
        }
    }
}
