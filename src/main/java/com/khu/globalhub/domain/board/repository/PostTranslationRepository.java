package com.khu.globalhub.domain.board.repository;

import com.khu.globalhub.domain.board.entity.PostTranslation;
import com.khu.globalhub.global.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostTranslationRepository extends JpaRepository<PostTranslation, Long> {

    Optional<PostTranslation> findByPostIdAndLanguage(Long postId, Language language);
}
