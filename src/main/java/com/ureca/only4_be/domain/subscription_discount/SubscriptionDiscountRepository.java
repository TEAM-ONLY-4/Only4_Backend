package com.ureca.only4_be.domain.subscription_discount;

import com.ureca.only4_be.domain.subscription.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionDiscountRepository extends JpaRepository<SubscriptionDiscount, Long> {
    // 할인 정책(DiscountPolicy) 정보까지 한 방에 가져옴 (할인율 확인용)
    // IN 절을 사용하여 여러 구독의 할인을 한 번에 조회
    @Query("SELECT sd FROM SubscriptionDiscount sd JOIN FETCH sd.discountPolicy WHERE sd.subscription IN :subscriptions")
    List<SubscriptionDiscount> findAllBySubscriptionIn(@Param("subscriptions") List<Subscription> subscriptions);
}
