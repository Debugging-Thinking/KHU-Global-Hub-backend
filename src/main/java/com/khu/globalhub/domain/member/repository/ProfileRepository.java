package com.khu.globalhub.domain.member.repository;

import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.shared.enums.MentoringRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    Optional<Profile> findByMemberId(Long memberId);

    boolean existsByMemberId(Long memberId);

    /** 특정 역할을 가진 프로필 전체 조회 (매칭 알고리즘용) */
    List<Profile> findByMentoringRole(MentoringRole role);

    /** 입학년도가 기준 연도 미만이고 역할이 MENTEE인 프로필 조회 (3월 자동 승격용) */
    List<Profile> findByAdmissionYearLessThanAndMentoringRole(int year, MentoringRole role);
}
