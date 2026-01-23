package com.ureca.only4_be.domain.bill_notification;

import com.ureca.only4_be.api.dto.NotificationStatDto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BillNotificationRepository extends JpaRepository<BillNotification, Long> {

    /**
     * [Step 2 Writer용]
     * Kafka 전송 완료 후, 상태를 PENDING -> PUBLISHING(또는 SENT)으로 변경
     */
    @Modifying(clearAutomatically = true)// 벌크 연산 후 영속성 컨텍스트 초기화
    @Query("UPDATE BillNotification bn " +
            "SET bn.publishStatus = :status " +
            "WHERE bn.id IN :ids")
    void updatePublishStatus(@Param("ids") List<Long> ids,
                             @Param("status") PublishStatus status);

    @Query(value = """
        SELECT 
            TO_CHAR(b.billing_year_month, 'YYYY-MM') AS billingMonth,
            COUNT(CASE WHEN bn.publish_status = 'PUBLISHED' THEN 1 END) AS publishCount,
            COUNT(CASE WHEN bn.send_status = 'SENT' THEN 1 END) AS sendCount
        FROM bill_notification bn
        JOIN bill b ON bn.bill_id = b.id
        GROUP BY TO_CHAR(b.billing_year_month, 'YYYY-MM')
        ORDER BY billingMonth DESC
        LIMIT 12
        """, nativeQuery = true)
    List<NotificationStatDto> findMonthlyStats();
}
