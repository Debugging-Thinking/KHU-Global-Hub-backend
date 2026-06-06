package com.khu.globalhub.shared.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Azure Translator API 호출만 담당하는 저수준 클라이언트. (BC 엔티티에 결합되지 않음)
 *
 * 두 곳에서 재사용된다:
 *  - {@link TranslationService} : 게시글/댓글/Q&A 작성 시 6개 언어 사전번역(@Async 영속화)
 *  - translation BC (POST /api/translate) : 6개 외 언어 사용자/채팅의 on-demand 번역
 *
 * from(원문 언어)을 지정하지 않으면 Azure가 자동 감지하며, 응답 detectedLanguage에 포함된다.
 */
@Slf4j
@Component
public class AzureTranslateClient {

    private final String key;
    private final String endpoint;
    private final String region;

    public AzureTranslateClient(
            @Value("${azure.translator.key}") String key,
            @Value("${azure.translator.endpoint}") String endpoint,
            @Value("${azure.translator.region}") String region) {
        this.key = key;
        this.endpoint = endpoint;
        this.region = region;
    }

    /**
     * 입력 텍스트별 번역 결과.
     * @param detectedLanguage Azure가 감지한 원문 언어 코드 (from 미지정 시), 없으면 null
     * @param translations toCodes 순서와 정렬된 번역문 (실패 항목은 null)
     */
    public record TranslatedText(String detectedLanguage, List<String> translations) {}

    /**
     * texts를 toCodes 각 언어로 번역한다.
     * @param fromCode 원문 언어 코드. null/blank면 Azure 자동 감지.
     * @return texts와 같은 길이의 결과 리스트. 입력이 비었거나 응답이 없으면 빈 리스트.
     */
    @SuppressWarnings("unchecked")
    public List<TranslatedText> translate(List<String> texts, List<String> toCodes, String fromCode) {
        if (texts == null || texts.isEmpty() || toCodes == null || toCodes.isEmpty()) {
            return List.of();
        }

        String toParams = toCodes.stream()
                .map(c -> "to=" + c)
                .reduce((a, b) -> a + "&" + b)
                .orElse("");
        String fromParam = (fromCode != null && !fromCode.isBlank()) ? "&from=" + fromCode : "";
        String url = endpoint + "/translate?api-version=3.0&" + toParams + fromParam;

        List<Map<String, String>> body = texts.stream()
                .map(t -> Map.of("Text", t == null ? "" : t))
                .toList();

        List<Map<String, Object>> raw = RestClient.create()
                .post()
                .uri(url)
                .header("Ocp-Apim-Subscription-Key", key)
                .header("Ocp-Apim-Subscription-Region", region)
                .header("Content-Type", "application/json")
                .body(body)
                .retrieve()
                .body(List.class);

        if (raw == null) return List.of();

        List<TranslatedText> out = new ArrayList<>(raw.size());
        for (Map<String, Object> item : raw) {
            String detected = null;
            Object d = item.get("detectedLanguage");
            if (d instanceof Map<?, ?> dm) {
                Object lang = ((Map<String, Object>) dm).get("language");
                if (lang != null) detected = lang.toString();
            }

            List<String> translations = new ArrayList<>();
            Object tr = item.get("translations");
            if (tr instanceof List<?> trl) {
                for (Object o : trl) {
                    if (o instanceof Map<?, ?> tm) {
                        Object text = ((Map<String, Object>) tm).get("text");
                        translations.add(text == null ? null : text.toString());
                    } else {
                        translations.add(null);
                    }
                }
            }
            out.add(new TranslatedText(detected, translations));
        }
        return out;
    }
}
