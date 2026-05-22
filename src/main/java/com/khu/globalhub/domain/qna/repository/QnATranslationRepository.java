package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.QnATranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QnATranslationRepository extends JpaRepository<QnATranslation, Long> {

    Optional<QnATranslation> findByQnaIdAndLanguage(Long qnaId, Language language);

    /** 가장 먼저 저장된 번역 행 = 원문 언어 판별용 */
    Optional<QnATranslation> findFirstByQnaIdOrderByIdAsc(Long qnaId);
}
