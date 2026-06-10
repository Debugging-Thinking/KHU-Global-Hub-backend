package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.domain.GuideCategory;
import com.khu.globalhub.campusguide.domain.GuideCategoryTranslation;
import com.khu.globalhub.campusguide.domain.GuideTip;
import com.khu.globalhub.campusguide.domain.GuideTipTranslation;
import com.khu.globalhub.campusguide.infrastructure.GuideCategoryRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideCategoryTranslationRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideTipRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideTipTranslationRepository;
import com.khu.globalhub.campusguide.presentation.dto.AdminGuideCategoryRequest;
import com.khu.globalhub.campusguide.presentation.dto.AdminGuideTipRequest;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 전용 학사 가이드 CRUD 유스케이스.
 * 카테고리/팁 각각 생성/수정 시 메타 저장 + 원문(KO 등) 번역 행 동기 저장 + 나머지 언어 비동기 번역.
 * 퀴즈(QuizAdminService)와 동일한 사전번역 패턴을 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideAdminService {

    private final GuideCategoryRepository categoryRepository;
    private final GuideCategoryTranslationRepository categoryTranslationRepository;
    private final GuideTipRepository tipRepository;
    private final GuideTipTranslationRepository tipTranslationRepository;
    private final GuideTranslationWriter translationWriter;

    // ── 카테고리 ─────────────────────────────────────────

    @Transactional
    public Long createCategory(AdminGuideCategoryRequest req) {
        GuideCategory category = categoryRepository.save(GuideCategory.builder()
                .badgeKey(req.badgeKey())
                .emoji(req.emoji())
                .color(req.color())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build());

        Language claimed = req.language() != null ? req.language() : Language.KO;
        categoryTranslationRepository.save(GuideCategoryTranslation.builder()
                .categoryId(category.getId())
                .language(claimed)
                .title(req.title())
                .build());

        // 나머지 언어 비동기 번역
        translationWriter.translateCategory(category.getId(), req.title(), claimed);
        return category.getId();
    }

    @Transactional
    public void updateCategory(Long categoryId, AdminGuideCategoryRequest req) {
        GuideCategory category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_CATEGORY_NOT_FOUND));
        category.updateMeta(req.badgeKey(), req.emoji(), req.color(),
                req.sortOrder() != null ? req.sortOrder() : category.getSortOrder());

        Language claimed = req.language() != null ? req.language() : Language.KO;

        // 원문(작성자 언어) 행 upsert (퀴즈 PUT과 동일)
        categoryTranslationRepository.findByCategoryIdAndLanguage(categoryId, claimed)
                .ifPresentOrElse(
                        row -> { row.updateContent(req.title()); categoryTranslationRepository.save(row); },
                        () -> categoryTranslationRepository.save(GuideCategoryTranslation.builder()
                                .categoryId(categoryId).language(claimed).title(req.title()).build()));

        // 나머지 언어 재번역(upsert)
        translationWriter.translateCategory(categoryId, req.title(), claimed);
    }

    @Transactional
    public void deleteCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new CustomException(ErrorCode.GUIDE_CATEGORY_NOT_FOUND);
        }
        // 하위 팁 + 팁 번역까지 정리 (DB FK ON DELETE CASCADE 보강 — 앱 레벨에서도 명시 제거)
        List<Long> tipIds = tipRepository.findByCategoryId(categoryId).stream().map(GuideTip::getId).toList();
        if (!tipIds.isEmpty()) {
            tipTranslationRepository.deleteByTipIdIn(tipIds);
        }
        tipRepository.deleteByCategoryId(categoryId);
        categoryTranslationRepository.deleteByCategoryId(categoryId);
        categoryRepository.deleteById(categoryId);
    }

    // ── 팁 ───────────────────────────────────────────────

    @Transactional
    public Long createTip(AdminGuideTipRequest req) {
        if (!categoryRepository.existsById(req.categoryId())) {
            throw new CustomException(ErrorCode.GUIDE_CATEGORY_NOT_FOUND);
        }
        GuideTip tip = tipRepository.save(GuideTip.builder()
                .categoryId(req.categoryId())
                .icon(req.icon())
                .link(req.link())
                .sortOrder(req.sortOrder() != null ? req.sortOrder() : 0)
                .build());

        Language claimed = req.language() != null ? req.language() : Language.KO;
        String content = req.content() != null ? req.content() : "";
        tipTranslationRepository.save(GuideTipTranslation.builder()
                .tipId(tip.getId())
                .language(claimed)
                .title(req.title())
                .content(content)
                .build());

        // 나머지 언어 비동기 번역
        translationWriter.translateTip(tip.getId(), req.title(), content, claimed);
        return tip.getId();
    }

    @Transactional
    public void updateTip(Long tipId, AdminGuideTipRequest req) {
        GuideTip tip = tipRepository.findById(tipId)
                .orElseThrow(() -> new CustomException(ErrorCode.GUIDE_TIP_NOT_FOUND));
        tip.updateMeta(req.icon(), req.link(),
                req.sortOrder() != null ? req.sortOrder() : tip.getSortOrder());

        Language claimed = req.language() != null ? req.language() : Language.KO;
        String content = req.content() != null ? req.content() : "";

        // 원문(작성자 언어) 행 upsert
        tipTranslationRepository.findByTipIdAndLanguage(tipId, claimed)
                .ifPresentOrElse(
                        row -> { row.updateContent(req.title(), content); tipTranslationRepository.save(row); },
                        () -> tipTranslationRepository.save(GuideTipTranslation.builder()
                                .tipId(tipId).language(claimed).title(req.title()).content(content).build()));

        // 나머지 언어 재번역(upsert)
        translationWriter.translateTip(tipId, req.title(), content, claimed);
    }

    @Transactional
    public void deleteTip(Long tipId) {
        if (!tipRepository.existsById(tipId)) {
            throw new CustomException(ErrorCode.GUIDE_TIP_NOT_FOUND);
        }
        tipTranslationRepository.deleteByTipId(tipId);
        tipRepository.deleteById(tipId);
    }
}
