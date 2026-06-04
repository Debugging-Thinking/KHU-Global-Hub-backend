package com.khu.globalhub.mentoring.infrastructure;

import com.khu.globalhub.mentoring.domain.MentorMenteeMatch;
import com.khu.globalhub.shared.enums.MatchStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MentorMenteeMatchRepository extends JpaRepository<MentorMenteeMatch, Long> {

    /** 특정 학기에 이미 매칭된 멘티인지 확인 (중복 매칭 방지) */
    boolean existsByMenteeIdAndSemester(Long menteeId, String semester);

    /** 특정 학기에 이미 매칭된 멘토인지 확인 */
    boolean existsByMentorIdAndSemester(Long mentorId, String semester);

    /** 특정 학기의 멘토별 매칭된 멘티 수 조회 */
    @Query("SELECT m.mentorId, COUNT(m) FROM MentorMenteeMatch m " +
           "WHERE m.semester = :semester AND m.status = 'ACTIVE' " +
           "GROUP BY m.mentorId")
    List<Object[]> countMenteesByMentorForSemester(@Param("semester") String semester);

    /** 멘티 기준 매칭 이력 조회 */
    List<MentorMenteeMatch> findByMenteeIdOrderByMatchedAtDesc(Long menteeId);

    /** 멘토 기준 매칭 이력 조회 */
    List<MentorMenteeMatch> findByMentorIdOrderByMatchedAtDesc(Long mentorId);

    /** 해당 멤버(멘토 or 멘티)의 현재 ACTIVE 매칭 목록 조회 */
    @Query("SELECT m FROM MentorMenteeMatch m " +
           "WHERE (m.mentorId = :memberId OR m.menteeId = :memberId) " +
           "AND m.status = :status " +
           "ORDER BY m.matchedAt DESC")
    List<MentorMenteeMatch> findActiveMatchesByMemberId(
            @Param("memberId") Long memberId,
            @Param("status") MatchStatus status);

    /** 해당 멤버의 전체 매칭 이력 조회 (상태 무관) */
    @Query("SELECT m FROM MentorMenteeMatch m " +
           "WHERE (m.mentorId = :memberId OR m.menteeId = :memberId) " +
           "ORDER BY m.matchedAt DESC")
    List<MentorMenteeMatch> findAllMatchesByMemberId(@Param("memberId") Long memberId);
}
