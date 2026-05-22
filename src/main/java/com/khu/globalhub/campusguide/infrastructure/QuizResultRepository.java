package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<QuizResult> findTopByMemberIdOrderByScoreDesc(Long memberId);
}
