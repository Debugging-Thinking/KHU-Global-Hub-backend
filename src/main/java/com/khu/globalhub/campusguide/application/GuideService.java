package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.domain.GuideCategory;
import com.khu.globalhub.campusguide.domain.GuideCategoryTranslation;
import com.khu.globalhub.campusguide.domain.GuideTip;
import com.khu.globalhub.campusguide.domain.GuideTipTranslation;
import com.khu.globalhub.campusguide.infrastructure.GuideCategoryRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideCategoryTranslationRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideTipRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideTipTranslationRepository;
import com.khu.globalhub.campusguide.presentation.dto.GuideResponse;
import com.khu.globalhub.shared.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 학사 가이드 조회 유스케이스.
 * 카테고리(정렬순서) → 각 카테고리의 팁(정렬순서)을 트리로 조립하고,
 * 텍스트는 요청 언어 번역 행에서 선택한다(없으면 KO → 첫 행 폴백 — 퀴즈와 동일).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GuideService {

    private final GuideCategoryRepository categoryRepository;
    private final GuideCategoryTranslationRepository categoryTranslationRepository;
    private final GuideTipRepository tipRepository;
    private final GuideTipTranslationRepository tipTranslationRepository;

    public List<GuideResponse> getGuide(Language language) {
        List<GuideCategory> categories = categoryRepository.findAllByOrderBySortOrderAsc();
        if (categories.isEmpty()) return List.of();
        List<Long> categoryIds = categories.stream().map(GuideCategory::getId).toList();

        // 카테고리 번역 (N+1 방지)
        Map<Long, List<GuideCategoryTranslation>> catTrByCat =
                categoryTranslationRepository.findByCategoryIdIn(categoryIds).stream()
                        .collect(Collectors.groupingBy(GuideCategoryTranslation::getCategoryId));

        // 팁 (정렬순서 오름차순) — 카테고리별 그룹핑
        List<GuideTip> tips = tipRepository.findByCategoryIdInOrderBySortOrderAsc(categoryIds);
        Map<Long, List<GuideTip>> tipsByCat = tips.stream()
                .collect(Collectors.groupingBy(GuideTip::getCategoryId));

        // 팁 번역 (N+1 방지)
        List<Long> tipIds = tips.stream().map(GuideTip::getId).toList();
        Map<Long, List<GuideTipTranslation>> tipTrByTip = tipIds.isEmpty()
                ? Map.of()
                : tipTranslationRepository.findByTipIdIn(tipIds).stream()
                        .collect(Collectors.groupingBy(GuideTipTranslation::getTipId));

        return categories.stream().map(cat -> {
            GuideCategoryTranslation catTr = pickCategory(catTrByCat.getOrDefault(cat.getId(), List.of()), language);
            String title = catTr != null ? catTr.getTitle() : "";

            List<GuideResponse.Tip> tipDtos = tipsByCat.getOrDefault(cat.getId(), List.of()).stream()
                    .map(tip -> {
                        GuideTipTranslation tipTr = pickTip(tipTrByTip.getOrDefault(tip.getId(), List.of()), language);
                        return new GuideResponse.Tip(
                                tip.getId(),
                                tip.getIcon(),
                                tip.getLink(),
                                tipTr != null ? tipTr.getTitle() : "",
                                tipTr != null ? tipTr.getContent() : "");
                    }).toList();

            return new GuideResponse(cat.getId(), cat.getBadgeKey(), cat.getEmoji(), cat.getColor(), title, tipDtos);
        }).toList();
    }

    /** 요청 언어 → KO → 첫 행(원문) 순으로 카테고리 번역 행 선택. */
    private GuideCategoryTranslation pickCategory(List<GuideCategoryTranslation> trs, Language language) {
        return trs.stream().filter(t -> t.getLanguage() == language).findFirst()
                .or(() -> trs.stream().filter(t -> t.getLanguage() == Language.KO).findFirst())
                .or(() -> trs.stream().min(Comparator.comparing(GuideCategoryTranslation::getId)))
                .orElse(null);
    }

    /** 요청 언어 → KO → 첫 행(원문) 순으로 팁 번역 행 선택. */
    private GuideTipTranslation pickTip(List<GuideTipTranslation> trs, Language language) {
        return trs.stream().filter(t -> t.getLanguage() == language).findFirst()
                .or(() -> trs.stream().filter(t -> t.getLanguage() == Language.KO).findFirst())
                .or(() -> trs.stream().min(Comparator.comparing(GuideTipTranslation::getId)))
                .orElse(null);
    }
}
