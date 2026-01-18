package com.ureca.only4_be.domain.subscription_usage;

import com.ureca.only4_be.domain.subscription.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubscriptionUsageRepository extends JpaRepository<SubscriptionUsage, Long> {
    // 여러 구독의 사용량을 한 번에 조회 (IN 쿼리)
    List<SubscriptionUsage> findAllBySubscriptionIn(List<Subscription> subscriptions);
}