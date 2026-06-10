package com.khu.globalhub.campusguide.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khu.globalhub.campusguide.application.QuizOptionsCodec;
import com.khu.globalhub.campusguide.domain.QuizQuestion;
import com.khu.globalhub.campusguide.domain.QuizQuestionTranslation;
import com.khu.globalhub.shared.enums.Language;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 로컬/최초 기동 시 퀴즈 문항 시드 (JSON 시드 파일 기반).
 *
 * <p>언어 무관 메타({@code seed/content_meta.json})로 {@link QuizQuestion}(category/answerIndex)을 만들고,
 * 존재하는 언어별 텍스트 파일({@code seed/content_<lang>.json})마다 {@link QuizQuestionTranslation}을 채운다.
 * (시드 JSON은 프론트 {@code extract_seed.ts} + 번역 도구가 생성 — 기동 시 Azure 호출은 하지 않음)
 *
 * <p>아직 번역이 끝나지 않은 언어 파일은 없을 수 있으므로 <b>존재하는 파일만</b> 로드한다.
 * meta의 quiz 키(예 {@code "1"})로 저장된 문항 ID를 매핑해 각 언어 번역 행을 연결한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class QuizDataInitializer implements ApplicationRunner {

    /** 로드 시도 언어 코드 — Language enum 이름(대문자)과 1:1 (ko→KO ... mn→MN). */
    private static final String[] LANG_CODES = {"ko", "en", "zh", "vi", "uz", "mn"};

    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionTranslationRepository translationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (questionRepository.count() > 0) return; // 이미 시드됨 — 중복 방지

        JsonNode meta = loadJson("content_meta.json");
        if (meta == null) {
            log.warn("[seed] seed/content_meta.json 없음 — 퀴즈 시드 스킵");
            return;
        }
        JsonNode quizMeta = meta.get("quiz");
        if (quizMeta == null || !quizMeta.isObject()) return;

        // 메타로 문항 생성 + seed 키(JSON 키) → 저장된 QuizQuestion.id 매핑 (번역 행 연결용)
        Map<String, Long> idBySeedKey = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> metaIt = quizMeta.fields();
        while (metaIt.hasNext()) {
            Map.Entry<String, JsonNode> e = metaIt.next();
            JsonNode m = e.getValue();
            QuizQuestion saved = questionRepository.save(QuizQuestion.builder()
                    .category(text(m, "category"))
                    .answerIndex(intVal(m, "answerIndex"))
                    .build());
            idBySeedKey.put(e.getKey(), saved.getId());
        }

        // 존재하는 언어 파일마다 번역 행 저장 (ko 우선 — 첫 행=원문 폴백 기준)
        for (String code : LANG_CODES) {
            JsonNode langRoot = loadJson("content_" + code + ".json");
            if (langRoot == null) continue;
            JsonNode quizText = langRoot.get("quiz");
            if (quizText == null || !quizText.isObject()) continue;
            Language language = Language.valueOf(code.toUpperCase());

            Iterator<Map.Entry<String, JsonNode>> textIt = quizText.fields();
            while (textIt.hasNext()) {
                Map.Entry<String, JsonNode> e = textIt.next();
                Long questionId = idBySeedKey.get(e.getKey());
                if (questionId == null) continue; // 메타에 없는 키는 무시
                JsonNode t = e.getValue();
                translationRepository.save(QuizQuestionTranslation.builder()
                        .questionId(questionId)
                        .language(language)
                        .question(text(t, "question"))
                        .optionsJson(QuizOptionsCodec.serialize(stringList(t.get("options"))))
                        .explanation(text(t, "explanation"))
                        .build());
            }
        }
    }

    /** classpath {@code seed/<fileName>} 로드. 파일 없거나 읽기 실패면 null (아직 안 끝난 언어 대비). */
    private JsonNode loadJson(String fileName) {
        ClassPathResource resource = new ClassPathResource("seed/" + fileName);
        if (!resource.exists()) return null;
        try (InputStream in = resource.getInputStream()) {
            return objectMapper.readTree(in);
        } catch (IOException ex) {
            log.warn("[seed] seed/{} 로드 실패 — 스킵", fileName, ex);
            return null;
        }
    }

    /** 문자열 필드 — 없거나 null이면 null. */
    private static String text(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? null : v.asText();
    }

    /** 정수 필드 — 없거나 null이면 0. */
    private static int intVal(JsonNode node, String field) {
        JsonNode v = node.get(field);
        return (v == null || v.isNull()) ? 0 : v.asInt();
    }

    /** JSON 배열 → 문자열 리스트 (보기 options). 배열 아니면 빈 리스트. */
    private static List<String> stringList(JsonNode arr) {
        List<String> out = new ArrayList<>();
        if (arr != null && arr.isArray()) {
            arr.forEach(n -> out.add(n.asText()));
        }
        return out;
    }
}
