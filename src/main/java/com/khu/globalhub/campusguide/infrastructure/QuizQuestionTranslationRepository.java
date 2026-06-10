package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.QuizQuestionTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuizQuestionTranslationRepository extends JpaRepository<QuizQuestionTranslation, Long> {

    Optional<QuizQuestionTranslation> findByQuestionIdAndLanguage(Long questionId, Language language);

    /** 원문(소스) 행 — 가장 먼저 저장된 번역 행. 폴백 산출에 사용. */
    Optional<QuizQuestionTranslation> findFirstByQuestionIdOrderByIdAsc(Long questionId);

    /** 문항 목록 조회 시 여러 문항의 번역을 한 번에 (N+1 방지). */
    List<QuizQuestionTranslation> findByQuestionIdIn(Collection<Long> questionIds);

    /** 문항 삭제/수정 시 기존 번역 행 일괄 제거. */
    void deleteByQuestionId(Long questionId);
}
