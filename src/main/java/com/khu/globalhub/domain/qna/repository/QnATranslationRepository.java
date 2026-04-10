package com.khu.globalhub.domain.qna.repository;

import com.khu.globalhub.domain.qna.entity.QnATranslation;
import com.khu.globalhub.global.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface QnATranslationRepository extends JpaRepository<QnATranslation, Long> {

    Optional<QnATranslation> findByQnaIdAndLanguage(Long qnaId, Language language);
}
