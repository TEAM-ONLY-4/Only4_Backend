package com.ureca.only4_be.domain.one_time_purchase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface OneTimePurchaseRepository extends JpaRepository<OneTimePurchase, Long> {

    /**
     * [배치용 Bulk Fetch 쿼리]
     * 특별한 연관관계 Fetch가 필요 없다면 메소드 이름만으로 IN 절 쿼리 생성 가능.
     */
    List<OneTimePurchase> findAllByMemberIdIn(List<Long> memberIds);
}