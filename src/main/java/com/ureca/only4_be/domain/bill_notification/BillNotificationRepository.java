package com.ureca.only4_be.domain.bill_notification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BillNotificationRepository extends JpaRepository<BillNotification, Long> {

    /**
     * [Step 2 Writer용]
     * Kafka 전송 완료 후, 상태를 PENDING -> PUBLISHING(또는 SENT)으로 변경
     */
    @Modifying(clearAutomatically = true)// 벌크 연산 후 영속성 컨텍스트 초기화
    @Query("UPDATE BillNotification bn" +
            "SET bn.publishStatus = 'PUBLISHING'+" +
            "    bn.processStartTime = :now, "+// 처리 시작/완료 시간 갱신
            "WHERE bn.id IN :ids")
    void updatePublishStatus(@Param("ids") List<Long> ids,
                             @Param("status") PublishStatus status,
                             @Param("now") LocalDateTime now);
}
