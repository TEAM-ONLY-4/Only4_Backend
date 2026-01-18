package com.ureca.only4_be.domain.one_time_purchase;

import com.ureca.only4_be.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OneTimePurchaseRepository extends JpaRepository<OneTimePurchase, Long> {
    List<OneTimePurchase> findAllByMember(Member member);
}