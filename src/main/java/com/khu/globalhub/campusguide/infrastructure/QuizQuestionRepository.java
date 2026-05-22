package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.QuizQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuizQuestionRepository extends JpaRepository<QuizQuestion, Long> {
    List<QuizQuestion> findByCategory(String category);
    boolean existsByQuestion(String question);
}
