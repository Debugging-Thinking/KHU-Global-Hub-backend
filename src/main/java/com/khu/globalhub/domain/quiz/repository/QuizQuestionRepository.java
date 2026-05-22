package com.khu.globalhub.domain.quiz.repository;

import com.khu.globalhub.domain.quiz.entity.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByCategory(String category);
    boolean existsByQuestion(String question);
}
