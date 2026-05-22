package com.khu.globalhub.domain.mentoring.repository;

import com.khu.globalhub.domain.mentoring.entity.MentorMenteeMatch;
import com.khu.globalhub.shared.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MentorMenteeMatchRepository extends JpaRepository<MentorMenteeMatch, Long> {

    /** 특정 학기에 이미 매칭된 멘티인지 확인 (중복 매칭 방지) */
    boolean existsByMenteeIdAndSemester(Long menteeId, String semester);

/** 멘티 기준 매칭 이력 조회 */
    List<MentorMenteeMatch> findByMenteeIdOrderByMatchedAtDesc(Long menteeId);

    /** 멘토 기준 매칭 이력 조회 */
    List<MentorMenteeMatch> findByMentorIdOrderByMatchedAtDesc(Long mentorId);

    /** 해당 멤버(멘토 or 멘티)의 현재 ACTIVE 매칭 목록 조회 (멘토는 복수 가능) */
    @Query("SELECT m FROM MentorMenteeMatch m " +
           "WHERE (m.mentor.id = :memberId OR m.mentee.id = :memberId) " +
           "AND m.status = :status " +
           "ORDER BY m.matchedAt DESC")
    List<MentorMenteeMatch> findActiveMatchesByMemberId(
            @Param("memberId") Long memberId,
            @Param("status") MatchStatus status);
}
