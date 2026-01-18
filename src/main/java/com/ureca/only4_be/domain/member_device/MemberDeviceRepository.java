package com.ureca.only4_be.domain.member_device;

import com.ureca.only4_be.domain.member.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MemberDeviceRepository extends JpaRepository<MemberDevice, Long> {
    // 기기(Device) 스펙 정보까지 한 방에 가져옴 (기기명, 출고가 등 확인용)
    @Query("SELECT md FROM MemberDevice md JOIN FETCH md.device WHERE md.member = :member")
    List<MemberDevice> findAllByMember(@Param("member") Member member);
}
