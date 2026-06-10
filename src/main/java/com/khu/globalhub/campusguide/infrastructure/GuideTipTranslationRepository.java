package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.domain.GuideTipTranslation;
import com.khu.globalhub.shared.enums.Language;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface GuideTipTranslationRepository extends JpaRepository<GuideTipTranslation, Long> {

    Optional<GuideTipTranslation> findByTipIdAndLanguage(Long tipId, Language language);

    /** 원문(소스) 행 — 가장 먼저 저장된 번역 행. 폴백 산출에 사용. */
    Optional<GuideTipTranslation> findFirstByTipIdOrderByIdAsc(Long tipId);

    /** 가이드 트리 조립 시 여러 팁의 번역을 한 번에 (N+1 방지). */
    List<GuideTipTranslation> findByTipIdIn(Collection<Long> tipIds);

    /** 팁 삭제/수정 시 기존 번역 행 일괄 제거. */
    void deleteByTipId(Long tipId);

    /** 카테고리 삭제 시 하위 팁들의 번역을 일괄 제거. */
    void deleteByTipIdIn(Collection<Long> tipIds);
}
