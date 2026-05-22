package com.khu.globalhub.board.infrastructure;

import com.khu.globalhub.board.domain.PostTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostTranslationRepository extends JpaRepository<PostTranslation, Long> {

    Optional<PostTranslation> findByPostIdAndLanguage(Long postId, Language language);

    /** 가장 먼저 저장된 번역 행 = 원문 언어 판별용 */
    Optional<PostTranslation> findFirstByPostIdOrderByIdAsc(Long postId);
}
