package com.ureca.only4_be.domain.member;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface MemberRepository extends JpaRepository<Member, Long> {

    // ★ [범위 파티셔닝] + [커서 기반] + [중복 방지] 모두 포함된 쿼리
    @Query("SELECT m FROM Member m " +
            "WHERE m.id > :lastId " +       // 1. 커서 (이전 페이지 다음부터)
            "AND m.id <= :maxId " +         // 2. 파티셔닝 (내 구역 끝까지만)
            "AND NOT EXISTS (" +            // 3. 중복 방지 (이미 이번 달 청구서 있으면 제외)
            "   SELECT 1 FROM Bill b " +
            "   WHERE b.member = m " +
            "   AND b.billingYearMonth = :targetDate" +
            ") " +
            "ORDER BY m.id ASC")
    Slice<Member> findMembersByCursorWithRangeAndFilter(
            @Param("lastId") Long lastId,
            @Param("maxId") Long maxId,
            @Param("targetDate") LocalDate targetDate,
            Pageable pageable
    );
}