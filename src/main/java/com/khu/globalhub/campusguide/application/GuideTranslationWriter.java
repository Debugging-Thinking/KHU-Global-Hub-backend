package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.domain.GuideCategoryTranslation;
import com.khu.globalhub.campusguide.domain.GuideTipTranslation;
import com.khu.globalhub.campusguide.infrastructure.GuideCategoryTranslationRepository;
import com.khu.globalhub.campusguide.infrastructure.GuideTipTranslationRepository;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.infra.AzureTranslateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 학사 가이드 비동기 사전번역 — 퀴즈(QuizTranslationWriter)와 동일한 방식.
 *
 * 카테고리는 텍스트 1개(title), 팁은 텍스트 2개(title + content)를 한 번에 번역 요청하고
 * 입력 인덱스로 결과를 분해한다(언어별 번역 호출 1회).
 *
 * 원문 언어 자동 감지(title 기준) → 원문 행 라벨 보정 → 나머지 언어 행 upsert.
 * 실패 시 조용히 무시(해당 언어 행 없음 → 조회 시 KO 폴백).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GuideTranslationWriter {

    private final AzureTranslateClient azureClient;
    private final GuideCategoryTranslationRepository categoryTranslationRepository;
    private final GuideTipTranslationRepository tipTranslationRepository;

    private static final List<Language> ALL = Arrays.asList(Language.values());

    private static List<String> targetCodes() {
        return ALL.stream().map(Language::toAzureCode).toList();
    }

    /** 카테고리 제목(title) 1개를 6개 언어로 번역해 upsert. */
    @Async("translationExecutor")
    public void translateCategory(Long categoryId, String title, Language claimed) {
        try {
            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(List.of(title), targetCodes(), null);
            if (results.isEmpty()) return;

            String detected = results.get(0).detectedLanguage();
            Language source = Language.fromAzureCode(detected).orElse(claimed);
            if (source != claimed) {
                categoryTranslationRepository.findByCategoryIdAndLanguage(categoryId, claimed)
                        .ifPresent(orig -> { orig.updateLanguage(source); categoryTranslationRepository.save(orig); });
            }

            for (int li = 0; li < ALL.size(); li++) {
                Language lang = ALL.get(li);
                if (lang == source) continue; // 원문 언어 행은 동기 저장본 유지

                String tTitle = at(results.get(0).translations(), li);
                if (tTitle == null) continue;

                final Language l = lang;
                final String fTitle = tTitle;
                categoryTranslationRepository.findByCategoryIdAndLanguage(categoryId, l).ifPresentOrElse(
                        row -> { row.updateContent(fTitle); categoryTranslationRepository.save(row); },
                        () -> categoryTranslationRepository.save(GuideCategoryTranslation.builder()
                                .categoryId(categoryId).language(l).title(fTitle).build()));
            }
        } catch (Exception e) {
            log.warn("Guide category translation failed [categoryId={}]: {}", categoryId, e.getMessage());
        }
    }

    /** 팁 제목(title) + 본문(content)을 한 번에 6개 언어로 번역해 upsert. */
    @Async("translationExecutor")
    public void translateTip(Long tipId, String title, String content, Language claimed) {
        try {
            boolean hasContent = content != null && !content.isBlank();

            // 입력 구성: [0]=title, [1]=content?
            List<String> inputs = new ArrayList<>();
            inputs.add(title);
            if (hasContent) inputs.add(content);

            List<AzureTranslateClient.TranslatedText> results =
                    azureClient.translate(inputs, targetCodes(), null);
            if (results.isEmpty()) return;

            String detected = results.get(0).detectedLanguage();
            Language source = Language.fromAzureCode(detected).orElse(claimed);
            if (source != claimed) {
                tipTranslationRepository.findByTipIdAndLanguage(tipId, claimed)
                        .ifPresent(orig -> { orig.updateLanguage(source); tipTranslationRepository.save(orig); });
            }

            for (int li = 0; li < ALL.size(); li++) {
                Language lang = ALL.get(li);
                if (lang == source) continue; // 원문 언어 행은 동기 저장본 유지

                String tTitle = at(results.get(0).translations(), li);
                if (tTitle == null) continue; // 제목 번역 실패 시 해당 언어 스킵

                String tContent;
                if (hasContent) {
                    tContent = at(results.get(1).translations(), li);
                    if (tContent == null) continue; // 본문 번역 실패 시 해당 언어 스킵(본문 누락 방지)
                } else {
                    tContent = "";
                }

                final Language l = lang;
                final String fTitle = tTitle;
                final String fContent = tContent;
                tipTranslationRepository.findByTipIdAndLanguage(tipId, l).ifPresentOrElse(
                        row -> { row.updateContent(fTitle, fContent); tipTranslationRepository.save(row); },
                        () -> tipTranslationRepository.save(GuideTipTranslation.builder()
                                .tipId(tipId).language(l).title(fTitle).content(fContent).build()));
            }
        } catch (Exception e) {
            log.warn("Guide tip translation failed [tipId={}]: {}", tipId, e.getMessage());
        }
    }

    /** 인덱스 범위를 벗어나면 null. */
    private String at(List<String> list, int index) {
        return (list != null && index >= 0 && index < list.size()) ? list.get(index) : null;
    }
}
