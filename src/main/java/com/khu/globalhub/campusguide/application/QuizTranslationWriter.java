package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.domain.QuizQuestionTranslation;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionTranslationRepository;
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
 * 퀴즈 문항 비동기 사전번역 — 게시판/강의평과 동일한 방식.
 *
 * 한 문항은 텍스트가 여러 개(질문 1 + 보기 N + 해설 0~1)이므로
 * [question, option0, option1, ..., explanation?] 순서로 한 번에 번역 요청을 보내고
 * 입력 인덱스로 결과를 분해한다(번역 호출 1회로 모든 텍스트 처리).
 *
 * 원문 언어 자동 감지(질문 기준) → 원문 행 라벨 보정 → 나머지 언어 행 upsert.
 * 실패 시 조용히 무시(해당 언어 행 없음 → 조회 시 KO 폴백).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class QuizTranslationWriter {

    private final AzureTranslateClient azureClient;
    private final QuizQuestionTranslationRepository repository;

    private static final List<Language> ALL = Arrays.asList(Language.values());

    private static List<String> targetCodes() {
        return ALL.stream().map(Language::toAzureCode).toList();
    }

    @Async("translationExecutor")
    public void translate(Long questionId, String question, List<String> options, String explanation, Language claimed) {
        try {
            int optionCount = options == null ? 0 : options.size();
            boolean hasExplanation = explanation != null && !explanation.isBlank();

            // 입력 구성: [0]=question, [1..optionCount]=options, [optionCount+1]=explanation?
            List<String> inputs = new ArrayList<>();
            inputs.add(question);
            if (optionCount > 0) inputs.addAll(options);
            if (hasExplanation) inputs.add(explanation);

            List<AzureTranslateClient.TranslatedText> results = azureClient.translate(inputs, targetCodes(), null);
            if (results.isEmpty()) return;

            // 원문 언어 자동 감지(질문 기준) → claimed와 다르면 원문 행 라벨 보정
            String detected = results.get(0).detectedLanguage();
            Language source = Language.fromAzureCode(detected).orElse(claimed);
            if (source != claimed) {
                repository.findByQuestionIdAndLanguage(questionId, claimed)
                        .ifPresent(orig -> { orig.updateLanguage(source); repository.save(orig); });
            }

            for (int li = 0; li < ALL.size(); li++) {
                Language lang = ALL.get(li);
                if (lang == source) continue; // 원문 언어 행은 동기 저장본 유지

                String tQuestion = at(results.get(0).translations(), li);
                if (tQuestion == null) continue; // 질문 번역 실패 시 해당 언어 스킵

                // 보기 번역(하나라도 실패하면 해당 언어 스킵 — 보기 누락 방지)
                List<String> tOptions = new ArrayList<>(optionCount);
                boolean optionsOk = true;
                for (int oi = 0; oi < optionCount; oi++) {
                    String to = at(results.get(1 + oi).translations(), li);
                    if (to == null) { optionsOk = false; break; }
                    tOptions.add(to);
                }
                if (!optionsOk) continue;

                String tExplanation = hasExplanation ? at(results.get(1 + optionCount).translations(), li) : null;

                final Language l = lang;
                final String fQuestion = tQuestion;
                final String fOptionsJson = QuizOptionsCodec.serialize(tOptions);
                final String fExplanation = tExplanation;
                repository.findByQuestionIdAndLanguage(questionId, l).ifPresentOrElse(
                        row -> { row.updateContent(fQuestion, fOptionsJson, fExplanation); repository.save(row); },
                        () -> repository.save(QuizQuestionTranslation.builder()
                                .questionId(questionId).language(l)
                                .question(fQuestion).optionsJson(fOptionsJson).explanation(fExplanation)
                                .build()));
            }
        } catch (Exception e) {
            log.warn("Quiz translation failed [questionId={}]: {}", questionId, e.getMessage());
        }
    }

    /** 인덱스 범위를 벗어나면 null. */
    private String at(List<String> list, int index) {
        return (list != null && index >= 0 && index < list.size()) ? list.get(index) : null;
    }
}
