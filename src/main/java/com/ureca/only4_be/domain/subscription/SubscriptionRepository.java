package com.ureca.only4_be.domain.subscription;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    /**
     * [배치용 Bulk Fetch 쿼리]
     * 목표: 회원 1,000명의 구독 정보를 쿼리 1방으로 가져옴.
     * 1. IN 절 사용 -> N+1 해결
     * 2. JOIN FETCH s.product -> 상품(Product) 정보도 같이 로딩 (이름, 가격 등)
     */
    @Query("SELECT s FROM Subscription s " +
            "JOIN FETCH s.product " +
            "WHERE s.member.id IN :memberIds")
    List<Subscription> findAllByMemberIdIn(@Param("memberIds") List<Long> memberIds);
}