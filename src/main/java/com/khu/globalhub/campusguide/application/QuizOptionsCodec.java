package com.khu.globalhub.campusguide.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 퀴즈 보기(List&lt;String&gt;) ↔ JSON 문자열 직렬화 헬퍼.
 * 번역 행(quiz_question_translations.options)에 보기를 한 컬럼으로 저장하기 위해 사용한다.
 * Jackson ObjectMapper는 스레드 세이프하므로 정적 인스턴스를 공유한다.
 */
public final class QuizOptionsCodec {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<List<String>> LIST_OF_STRING = new TypeReference<>() {};

    private QuizOptionsCodec() {}

    /** 보기 리스트를 JSON 배열 문자열로 직렬화. null이면 빈 배열. */
    public static String serialize(List<String> options) {
        try {
            return MAPPER.writeValueAsString(options == null ? List.of() : options);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("퀴즈 보기 직렬화 실패", e);
        }
    }

    /** JSON 배열 문자열을 보기 리스트로 역직렬화. 비었거나 파싱 실패면 빈 리스트. */
    public static List<String> deserialize(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return MAPPER.readValue(json, LIST_OF_STRING);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
