package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.AnswerTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerTranslationRepository extends JpaRepository<AnswerTranslation, Long> {

    Optional<AnswerTranslation> findByAnswerIdAndLanguage(Long answerId, Language language);
}
