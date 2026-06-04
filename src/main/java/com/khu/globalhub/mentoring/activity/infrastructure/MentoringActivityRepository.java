package com.khu.globalhub.mentoring.activity.infrastructure;

import com.khu.globalhub.mentoring.activity.domain.MentoringActivity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MentoringActivityRepository extends JpaRepository<MentoringActivity, Long> {

    /** 특정 매칭의 활동 기록을 오름차순으로 조회 */
    List<MentoringActivity> findByMatchIdOrderByCreatedAtAsc(Long matchId);
}