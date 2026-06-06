package com.khu.globalhub.coursereview.infrastructure;

import com.khu.globalhub.coursereview.domain.CourseReviewTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseReviewTranslationRepository extends JpaRepository<CourseReviewTranslation, Long> {

    Optional<CourseReviewTranslation> findByReviewIdAndLanguage(Long reviewId, Language language);

    /** 원문(소스) 행 — 가장 먼저 저장된 번역 행. original=true / originalLanguage 산출에 사용. */
    Optional<CourseReviewTranslation> findFirstByReviewIdOrderByIdAsc(Long reviewId);

    /** 강의 상세에서 여러 강의평의 번역을 한 번에 (N+1 방지). */
    List<CourseReviewTranslation> findByReviewIdIn(Collection<Long> reviewIds);
}
