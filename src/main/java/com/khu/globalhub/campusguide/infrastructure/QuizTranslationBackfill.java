package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.campusguide.application.QuizOptionsCodec;
import com.khu.globalhub.campusguide.application.QuizTranslationWriter;
import com.khu.globalhub.campusguide.domain.QuizQuestion;
import com.khu.globalhub.campusguide.domain.QuizQuestionTranslation;
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
 * 기존 퀴즈 문항 번역 백필 — KO 등 일부 언어 행만 있는 문항을 6개 언어 전부로 채운다.
 *
 * <p><b>왜 필요한가:</b> {@code V16} 다국어 전환은 <i>이미 존재하던</i> 운영 문항을 KO 번역 행으로만 이관했고,
 * {@link QuizDataInitializer}는 {@code quiz_questions}가 비어있지 않으면 시드를 통째로 스킵한다.
 * 그 결과 운영의 기존 문항은 KO 행만 남아 비-한국어 사용자에게 번역이 안 된 것처럼 보였다.
 * (학사 가이드는 {@code V17}로 테이블이 새로 생겨 빈 상태에서 6개 언어가 모두 시드돼 정상이었다.)
 *
 * <p>이 러너는 기동 시 모든 문항의 번역 행을 점검해, 6개 언어를 다 갖추지 못한 문항만
 * 소스(KO 우선, 없으면 가장 먼저 저장된) 행을 기준으로 {@link QuizTranslationWriter}에 재번역을 위임한다.
 * writer가 upsert·언어 자동감지·실패 시 조용히 스킵을 모두 처리하므로 <b>여러 번 기동해도 안전(멱등)</b>하다.
 * 이미 6개 언어가 다 있는 문항(새 DB의 시드 문항 등)은 건너뛰므로 추가 Azure 호출이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.LOWEST_PRECEDENCE) // 시드(QuizDataInitializer) 이후 점검 — 순서 무관하게 동작하지만 명시
public class QuizTranslationBackfill implements ApplicationRunner {

    /** 앱이 지원하는 전체 언어(6개). 이 집합을 다 갖추지 못한 문항이 백필 대상. */
    private static final Set<Language> ALL_LANGUAGES = EnumSet.allOf(Language.class);

    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionTranslationRepository translationRepository;
    private final QuizTranslationWriter translationWriter;

    @Override
    public void run(ApplicationArguments args) {
        List<QuizQuestion> questions = questionRepository.findAll();
        if (questions.isEmpty()) return;

        Map<Long, List<QuizQuestionTranslation>> byQuestion = translationRepository
                .findByQuestionIdIn(questions.stream().map(QuizQuestion::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(QuizQuestionTranslation::getQuestionId));

        int dispatched = 0;
        for (QuizQuestion q : questions) {
            List<QuizQuestionTranslation> rows = byQuestion.getOrDefault(q.getId(), List.of());
            if (rows.isEmpty()) continue; // 소스 텍스트가 전혀 없으면 번역할 게 없음

            if (!needsBackfill(presentLanguages(rows))) continue; // 이미 6개 언어 완비 → 스킵

            QuizQuestionTranslation source = pickSource(rows);
            translationWriter.translate(
                    q.getId(),
                    source.getQuestion(),
                    QuizOptionsCodec.deserialize(source.getOptionsJson()),
                    source.getExplanation(),
                    source.getLanguage());
            dispatched++;
        }

        if (dispatched > 0) {
            log.info("[quiz-backfill] 번역 누락 문항 {}건 재번역 디스패치 (소스 → 나머지 언어)", dispatched);
        }
    }

    /** 보유 언어 집합이 지원 6개를 다 못 채우면 백필 대상. (순수 판정 — 단위 테스트 대상) */
    static boolean needsBackfill(Collection<Language> presentLanguages) {
        return !presentLanguages.containsAll(ALL_LANGUAGES);
    }

    private static Set<Language> presentLanguages(List<QuizQuestionTranslation> rows) {
        return rows.stream().map(QuizQuestionTranslation::getLanguage).collect(Collectors.toSet());
    }

    /** 소스(원문) 행 선택 — KO 우선, 없으면 가장 먼저 저장된(최소 id) 행. */
    private static QuizQuestionTranslation pickSource(List<QuizQuestionTranslation> rows) {
        return rows.stream().filter(r -> r.getLanguage() == Language.KO).findFirst()
                .orElseGet(() -> rows.stream()
                        .min(Comparator.comparing(QuizQuestionTranslation::getId))
                        .orElseThrow());
    }
}
