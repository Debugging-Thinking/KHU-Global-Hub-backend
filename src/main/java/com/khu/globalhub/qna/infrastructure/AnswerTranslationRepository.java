package com.khu.globalhub.qna.infrastructure;

import com.khu.globalhub.qna.domain.AnswerTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AnswerTranslationRepository extends JpaRepository<AnswerTranslation, Long> {

    Optional<AnswerTranslation> findByAnswerIdAndLanguage(Long answerId, Language language);

    /** 원문(소스) 행 — 가장 먼저 저장된 번역 행. original=true 조회에 사용. */
    Optional<AnswerTranslation> findFirstByAnswerIdOrderByIdAsc(Long answerId);
}
