package com.ureca.only4_be.domain.subscription;

import com.ureca.only4_be.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // 상품(Product) 정보까지 한 방에 가져옴
    @Query("SELECT s FROM Subscription s JOIN FETCH s.product WHERE s.member = :member AND s.subscriptionStatus = 'ACTIVE'")
    List<Subscription> findAllByMemberWithProduct(@Param("member") Member member);
}
