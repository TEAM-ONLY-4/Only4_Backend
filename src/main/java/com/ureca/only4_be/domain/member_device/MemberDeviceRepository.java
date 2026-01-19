package com.ureca.only4_be.domain.member_device;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {

    /**
     * [배치용 Bulk Fetch 쿼리]
     * 목표: 회원 1,000명의 기기 정보를 쿼리 1방으로 가져옴.
     * 1. WHERE ... IN :memberIds -> 1,000명분을 한 번에 조회 (N+1 해결)
     * 2. JOIN FETCH md.device -> 기기 스펙(Device) 정보도 같이 로딩 (추가 쿼리 방지)
     */
    @Query("SELECT md FROM MemberDevice md " +
            "JOIN FETCH md.device " +
            "WHERE md.member.id IN :memberIds")
    List<MemberDevice> findAllByMemberIdIn(@Param("memberIds") List<Long> memberIds);
}