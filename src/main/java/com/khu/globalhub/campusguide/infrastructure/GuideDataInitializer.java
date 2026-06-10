package com.khu.globalhub.campusguide.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khu.globalhub.campusguide.domain.GuideCategory;
import com.khu.globalhub.campusguide.domain.GuideCategoryTranslation;
import com.khu.globalhub.campusguide.domain.GuideTip;
import com.khu.globalhub.campusguide.domain.GuideTipTranslation;
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 로컬/최초 기동 시 학사 가이드 시드 (JSON 시드 파일 기반 — 퀴즈와 동일 패턴).
 *
 * <p>언어 무관 메타({@code seed/content_meta.json})로 {@link GuideCategory}(badgeKey=catId, emoji/color/order)와
 * 하위 {@link GuideTip}(icon/link/order)을 만들고, 존재하는 언어별 텍스트 파일마다
 * {@link GuideCategoryTranslation}(title) / {@link GuideTipTranslation}(title·content)을 채운다.
 *
 * <p>팁은 meta의 {@code guideTips} 키 {@code "<catId>#<i>"}로 소속 카테고리와 연결되며,
 * 시드 JSON은 프론트 {@code extract_seed.ts} + 번역 도구가 생성한다.
 * 아직 번역이 끝나지 않은 언어 파일은 <b>존재하는 파일만</b> 로드한다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GuideDataInitializer implements ApplicationRunner {

    /** 로드 시도 언어 코드 — Language enum 이름(대문자)과 1:1 (ko→KO ... mn→MN). */
    private static final String[] LANG_CODES = {"ko", "en", "zh", "vi", "uz", "mn"};

    private final GuideCategoryRepository categoryRepository;
    private final GuideCategoryTranslationRepository categoryTranslationRepository;
    private final GuideTipRepository tipRepository;
    private final GuideTipTranslationRepository tipTranslationRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categoryRepository.count() > 0) return; // 이미 시드됨 — 중복 방지

        JsonNode meta = loadJson("content_meta.json");
        if (meta == null) {
            log.warn("[seed] seed/content_meta.json 없음 — 가이드 시드 스킵");
            return;
        }

        // 1) 카테고리 메타 → GuideCategory 생성 + catId(badgeKey) → 저장된 id 매핑
        Map<String, Long> categoryIdByKey = new HashMap<>();
        JsonNode catMeta = meta.get("guideCategories");
        if (catMeta != null && catMeta.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = catMeta.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                JsonNode m = e.getValue();
                GuideCategory saved = categoryRepository.save(GuideCategory.builder()
                        .badgeKey(e.getKey())
                        .emoji(text(m, "emoji"))
                        .color(text(m, "color"))
                        .sortOrder(intVal(m, "order"))
                        .build());
                categoryIdByKey.put(e.getKey(), saved.getId());
            }
        }

        // 2) 팁 메타 (키 "<catId>#<i>") → GuideTip 생성 + seed 키 → 저장된 id 매핑
        Map<String, Long> tipIdBySeedKey = new HashMap<>();
        JsonNode tipMeta = meta.get("guideTips");
        if (tipMeta != null && tipMeta.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = tipMeta.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> e = it.next();
                Long categoryId = categoryIdByKey.get(categoryKeyOf(e.getKey()));
                if (categoryId == null) continue; // 소속 카테고리가 메타에 없으면 무시
                JsonNode m = e.getValue();
                GuideTip saved = tipRepository.save(GuideTip.builder()
                        .categoryId(categoryId)
                        .icon(text(m, "icon"))
                        .link(text(m, "link"))
                        .sortOrder(intVal(m, "order"))
                        .build());
                tipIdBySeedKey.put(e.getKey(), saved.getId());
            }
        }

        // 3) 존재하는 언어 파일마다 번역 행 저장 (ko 우선 — 첫 행=원문 폴백 기준)
        for (String code : LANG_CODES) {
            JsonNode langRoot = loadJson("content_" + code + ".json");
            if (langRoot == null) continue;
            Language language = Language.valueOf(code.toUpperCase());

            JsonNode catText = langRoot.get("guideCategories");
            if (catText != null && catText.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = catText.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    Long categoryId = categoryIdByKey.get(e.getKey());
                    if (categoryId == null) continue;
                    categoryTranslationRepository.save(GuideCategoryTranslation.builder()
                            .categoryId(categoryId)
                            .language(language)
                            .title(text(e.getValue(), "title"))
                            .build());
                }
            }

            JsonNode tipText = langRoot.get("guideTips");
            if (tipText != null && tipText.isObject()) {
                Iterator<Map.Entry<String, JsonNode>> it = tipText.fields();
                while (it.hasNext()) {
                    Map.Entry<String, JsonNode> e = it.next();
                    Long tipId = tipIdBySeedKey.get(e.getKey());
                    if (tipId == null) continue;
                    JsonNode t = e.getValue();
                    tipTranslationRepository.save(GuideTipTranslation.builder()
                            .tipId(tipId)
                            .language(language)
                            .title(text(t, "title"))
                            .content(text(t, "content"))
                            .build());
                }
            }
        }
    }

    /** {@code "COURSE_REG#0"} → {@code "COURSE_REG"} (마지막 '#' 앞 = 카테고리 키). */
    private static String categoryKeyOf(String tipSeedKey) {
        int idx = tipSeedKey.lastIndexOf('#');
        return idx >= 0 ? tipSeedKey.substring(0, idx) : tipSeedKey;
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
}
