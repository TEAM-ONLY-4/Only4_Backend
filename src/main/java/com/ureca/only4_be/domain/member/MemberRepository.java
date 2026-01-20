package com.ureca.only4_be.domain.member;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ★ 커서 기반 조회 쿼리 (핵심: m.id > :lastId)
    // "마지막으로 처리한 ID보다 큰 녀석들 중에서, 청구서 없는 애들 데려와"
    @Query("SELECT m FROM Member m " +
            "WHERE m.id > :lastId " + // <--- 여기가 핵심 커서 조건!
            "AND NOT EXISTS (" +
            "   SELECT 1 FROM Bill b " +
            "   WHERE b.member = m " +
            "   AND b.billingYearMonth = :targetDate" +
            ") " +
            "ORDER BY m.id ASC") // ID 순서대로 정렬 
    Slice<Member> findMembersByCursor(
            @Param("lastId") Long lastId,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );
}