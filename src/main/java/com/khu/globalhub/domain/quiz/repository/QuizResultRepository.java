package com.khu.globalhub.domain.quiz.repository;

import com.khu.globalhub.domain.quiz.entity.QuizResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuizResultRepository extends JpaRepository<QuizResult, Long> {
    List<QuizResult> findByMemberIdOrderByCreatedAtDesc(Long memberId);
    Optional<QuizResult> findTopByMemberIdOrderByScoreDesc(Long memberId);
}
