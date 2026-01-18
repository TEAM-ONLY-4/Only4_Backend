package com.ureca.only4_be.batch.jobs.notification.writer;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.BillRepository;
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
public class BillStatusUpdateWriter implements ItemWriter<NotificationRequest> {

    private final BillRepository billRepository;

    @Override
    public void write(Chunk<? extends NotificationRequest> chunk) {
        // Kafka 전송에 성공한 청구서 ID들만 추출
        List<Long> billIds = chunk.getItems().stream()
                .map(NotificationRequest::getBillId)
                .collect(Collectors.toList());

        //빈 리스트 체크 (방어 로직)
        if (billIds.isEmpty()) {
            return;
        }

        // 벌크 업데이트 실행 (1000건을 1쿼리로 처리)
        billRepository.updateBillStatusToSendComplete(billIds);

        // 5. 로그 기록
        log.info(">>> 💾 [DBWriter] 청구서 발송 상태 업데이트 완료: {} 건", billIds.size());
    }
}