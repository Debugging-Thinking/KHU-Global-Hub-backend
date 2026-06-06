package com.khu.globalhub.board.infrastructure;

import com.khu.globalhub.board.domain.CommentTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentTranslationRepository extends JpaRepository<CommentTranslation, Long> {

    Optional<CommentTranslation> findByCommentIdAndLanguage(Long commentId, Language language);

    /** 원문(소스) 행 — 가장 먼저 저장된 번역 행. original=true 조회에 사용. */
    Optional<CommentTranslation> findFirstByCommentIdOrderByIdAsc(Long commentId);
}
