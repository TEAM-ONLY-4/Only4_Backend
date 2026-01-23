package com.ureca.only4_be.domain.bill;

import com.ureca.only4_be.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    // 리스트에 있는 ID들의 상태를 'SENT(발송완료)'로 일괄 업데이트
    @Modifying(clearAutomatically = true) // 벌크 연산 후 영속성 컨텍스트 초기화
    @Query("UPDATE Bill b SET b.billSendStatus = 'SENT' WHERE b.id IN :ids")
    void updateBillStatusToSendComplete(@Param("ids") List<Long> ids);

    // 해당 월에 생성된 청구서 개수
    long countByBillingYearMonth(LocalDate billingYearMonth);

    // 해당 월의 총 청구 금액 합계
    @Query("SELECT SUM(b.totalBilledAmount) FROM Bill b WHERE b.billingYearMonth = :billingDate")
    BigDecimal sumTotalAmountByMonth(@Param("billingDate") LocalDate billingDate);
}
