package com.ureca.only4_be.batch.jobs.notification.listener;

import com.ureca.only4_be.batch.jobs.notification.dto.NotificationRequest;
import com.ureca.only4_be.domain.bill.Bill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.SkipListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationSkipListener implements SkipListener<Bill, NotificationRequest> {

    @Override
    public void onSkipInRead(Throwable t){
        log.error("읽기 중 요류 발생으로 건너뜀. 사유{}", t.getMessage());
    }
    @Override
    public void onSkipInProcess(Bill item, Throwable t){
        log.error("처리 중 요류 발생. Bill ID:{}, 사유:{}", item.getId(), t.getMessage());
    }
    @Override
    public void onSkipInWrite(NotificationRequest item, Throwable t){
        log.error("쓰기 중 오류 발생. Member ID:{}, 사유:{}", item.getMemberId(), t.getMessage());
    }
}
