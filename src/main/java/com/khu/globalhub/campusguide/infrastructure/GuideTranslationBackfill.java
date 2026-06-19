package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.application.GuideTranslationWriter;
import com.khu.globalhub.campusguide.domain.GuideCategory;
import com.khu.globalhub.campusguide.domain.GuideCategoryTranslation;
import com.khu.globalhub.campusguide.domain.GuideTip;
import com.khu.globalhub.campusguide.domain.GuideTipTranslation;
import com.khu.globalhub.shared.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 학사 가이드(카테고리/팁) 번역 백필 — {@link QuizTranslationBackfill}의 가이드 판.
 *
 * <p>가이드는 {@code V17}로 테이블이 새로 생겨 빈 상태에서 6개 언어가 모두 시드돼 보통 정상이지만,
 * 관리자가 만든 항목 중 번역이 실패해 일부 언어가 빠진 행이 있으면 비-한국어 사용자에게 KO로 보인다.
 * 기동 시 6개 언어를 다 갖추지 못한 카테고리/팁을 소스(KO 우선) 기준으로 재번역해 채운다.
 * writer가 upsert·자동감지·실패 스킵을 처리하므로 <b>멱등</b>이며, 이미 완비된 항목은 건너뛴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE)
public class GuideTranslationBackfill implements ApplicationRunner {

    private static final Set<Language> ALL_LANGUAGES = EnumSet.allOf(Language.class);

    private final GuideCategoryRepository categoryRepository;
    private final GuideTipRepository tipRepository;
    private final GuideCategoryTranslationRepository categoryTranslationRepository;
    private final GuideTipTranslationRepository tipTranslationRepository;
    private final GuideTranslationWriter translationWriter;

    @Override
    public void run(ApplicationArguments args) {
        int categories = backfillCategories();
        int tips = backfillTips();
        if (categories > 0 || tips > 0) {
            log.info("[guide-backfill] 재번역 디스패치 — 카테고리 {}건, 팁 {}건", categories, tips);
        }
    }

    private int backfillCategories() {
        List<GuideCategory> all = categoryRepository.findAllByOrderBySortOrderAsc();
        if (all.isEmpty()) return 0;

        Map<Long, List<GuideCategoryTranslation>> byCategory = categoryTranslationRepository
                .findByCategoryIdIn(all.stream().map(GuideCategory::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(GuideCategoryTranslation::getCategoryId));

        int dispatched = 0;
        for (GuideCategory c : all) {
            List<GuideCategoryTranslation> rows = byCategory.getOrDefault(c.getId(), List.of());
            if (rows.isEmpty()) continue;
            if (!needsBackfill(rows.stream().map(GuideCategoryTranslation::getLanguage).toList())) continue;

            GuideCategoryTranslation source = rows.stream().filter(r -> r.getLanguage() == Language.KO).findFirst()
                    .orElseGet(() -> rows.stream().min(Comparator.comparing(GuideCategoryTranslation::getId)).orElseThrow());
            translationWriter.translateCategory(c.getId(), source.getTitle(), source.getLanguage());
            dispatched++;
        }
        return dispatched;
    }

    private int backfillTips() {
        List<GuideTip> all = tipRepository.findAll();
        if (all.isEmpty()) return 0;

        Map<Long, List<GuideTipTranslation>> byTip = tipTranslationRepository
                .findByTipIdIn(all.stream().map(GuideTip::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(GuideTipTranslation::getTipId));

        int dispatched = 0;
        for (GuideTip tip : all) {
            List<GuideTipTranslation> rows = byTip.getOrDefault(tip.getId(), List.of());
            if (rows.isEmpty()) continue;
            if (!needsBackfill(rows.stream().map(GuideTipTranslation::getLanguage).toList())) continue;

            GuideTipTranslation source = rows.stream().filter(r -> r.getLanguage() == Language.KO).findFirst()
                    .orElseGet(() -> rows.stream().min(Comparator.comparing(GuideTipTranslation::getId)).orElseThrow());
            translationWriter.translateTip(tip.getId(), source.getTitle(), source.getContent(), source.getLanguage());
            dispatched++;
        }
        return dispatched;
    }

    /** 보유 언어 집합이 지원 6개를 다 못 채우면 백필 대상. */
    static boolean needsBackfill(Collection<Language> presentLanguages) {
        return !presentLanguages.containsAll(ALL_LANGUAGES);
    }
}
