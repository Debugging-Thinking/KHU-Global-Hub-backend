package com.khu.globalhub.coursereview.application;

import com.khu.globalhub.coursereview.domain.CourseReviewTranslation;
import com.khu.globalhub.coursereview.infrastructure.CourseReviewTranslationRepository;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.infra.AzureTranslateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 강의평 비동기 사전번역 — 게시판/댓글과 동일한 방식.
 * 원문 언어 자동 감지 → 원문 행 라벨 보정 → 나머지 언어 번역 저장.
 * 실패 시 조용히 무시(해당 언어 행 없음 → 조회 시 EN/원문 폴백).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CourseReviewTranslationWriter {

    private final AzureTranslateClient azureClient;
    private final CourseReviewTranslationRepository repository;

    private static final List<Language> ALL = Arrays.asList(Language.values());

    private static List<String> targetCodes() {
        return ALL.stream().map(Language::toAzureCode).toList();
    }

    @Async("translationExecutor")
    public void translate(Long reviewId, String content, Language claimed) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(List.of(content), targetCodes(), null);
            if (results.isEmpty()) return;

            String detected = results.get(0).detectedLanguage();
            Language source = Language.fromAzureCode(detected).orElse(claimed);
            if (source != claimed) {
                repository.findByReviewIdAndLanguage(reviewId, claimed)
                        .ifPresent(orig -> { orig.updateLanguage(source); repository.save(orig); });
            }

            List<String> translations = results.get(0).translations();
            for (int i = 0; i < ALL.size(); i++) {
                Language lang = ALL.get(i);
                if (lang == source) continue;
                String translated = (translations != null && i < translations.size()) ? translations.get(i) : null;
                if (translated != null) {
                    final Language l = lang;
                    repository.findByReviewIdAndLanguage(reviewId, l).ifPresentOrElse(
                            row -> { row.updateContent(translated); repository.save(row); },
                            () -> repository.save(CourseReviewTranslation.builder()
                                    .reviewId(reviewId).language(l).content(translated).build()));
                }
            }
        } catch (Exception e) {
            log.warn("CourseReview translation failed [reviewId={}]: {}", reviewId, e.getMessage());
        }
    }
}
