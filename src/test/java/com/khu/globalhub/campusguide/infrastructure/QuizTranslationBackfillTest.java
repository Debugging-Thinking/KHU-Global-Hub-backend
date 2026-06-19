package com.khu.globalhub.campusguide.infrastructure;

import com.khu.globalhub.shared.enums.Language;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 퀴즈 번역 백필 판정 로직 단위 테스트.
 * 운영 버그(기존 문항이 KO 행만 남아 비-한국어 사용자에게 번역 안 됨)의 회귀 가드.
 */
@DisplayName("[unit] 퀴즈 번역 백필 판정")
class QuizTranslationBackfillTest {

    @Test
    @DisplayName("KO 행만 있는 문항 → 백필 대상 (운영 버그 재현)")
    void koOnlyNeedsBackfill() {
        assertThat(QuizTranslationBackfill.needsBackfill(Set.of(Language.KO))).isTrue();
    }

    @Test
    @DisplayName("일부 언어만 있는 문항 → 백필 대상")
    void partialNeedsBackfill() {
        assertThat(QuizTranslationBackfill.needsBackfill(List.of(Language.KO, Language.EN, Language.ZH)))
                .isTrue();
    }

    @Test
    @DisplayName("6개 언어 모두 있는 문항 → 백필 불필요 (불필요한 Azure 호출 방지)")
    void fullyTranslatedSkipsBackfill() {
        assertThat(QuizTranslationBackfill.needsBackfill(EnumSet.allOf(Language.class))).isFalse();
    }

    @Test
    @DisplayName("번역 행이 전혀 없는 문항 → 백필 대상")
    void emptyNeedsBackfill() {
        assertThat(QuizTranslationBackfill.needsBackfill(Set.of())).isTrue();
    }
}
