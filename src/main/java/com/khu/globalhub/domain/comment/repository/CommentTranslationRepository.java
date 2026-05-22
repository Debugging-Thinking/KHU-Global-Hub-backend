package com.khu.globalhub.domain.comment.repository;

import com.khu.globalhub.domain.comment.entity.CommentTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CommentTranslationRepository extends JpaRepository<CommentTranslation, Long> {

    Optional<CommentTranslation> findByCommentIdAndLanguage(Long commentId, Language language);
}
