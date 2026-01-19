package com.ureca.only4_be.domain.member;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // 청구서가 없는 회원만 페이징으로 가져오기 (Processor 검사 대체)
    @Query("SELECT m FROM Member m " +
            "WHERE NOT EXISTS (" +
            "   SELECT 1 FROM Bill b " +
            "   WHERE b.member = m " +
            "   AND b.billingYearMonth = :targetDate" +
            ")")
    Slice<Member> findMembersWithoutBill(@Param("targetDate") LocalDate targetDate, Pageable pageable);
}