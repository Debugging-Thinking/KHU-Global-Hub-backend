package com.khu.globalhub.characterization;

import com.fasterxml.jackson.databind.JsonNode;
import com.khu.globalhub.AbstractIntegrationTest;
import com.khu.globalhub.shared.infra.S3Service;
import com.khu.globalhub.shared.infra.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 학사 퀴즈(/api/quiz) 플로우 현재 동작 박제 (characterization).
 * 문항은 QuizDataInitializer가 시드한다(TRUNCATE 제외 테이블).
 */
@DisplayName("[characterization] 퀴즈 플로우")
class QuizFlowTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 모든 characterization 클래스가 동일한 @MockitoBean 셋을 선언해 단일 Spring 컨텍스트(=단일 컨테이너)를 공유한다.
    @MockitoBean
    TranslationService translationService;
    @MockitoBean
    S3Service s3Service;

    private long myMemberId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/members/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).get("memberId").asLong();
    }

    private JsonNode fetchQuestions(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/quiz/questions")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result);
    }

    /** 시드된 정답 인덱스를 questionId → answerIndex 맵으로 직접 조회. */
    private Map<Long, Integer> seededAnswers() {
        Map<Long, Integer> map = new java.util.HashMap<>();
        jdbcTemplate.query("SELECT id, answer_index FROM quiz_questions", rs -> {
            map.put(rs.getLong("id"), rs.getInt("answer_index"));
        });
        return map;
    }

    @Test
    @DisplayName("문제 목록 조회: 시드 18문항, 정답(answerIndex) 미노출, options 배열 포함")
    void getQuestions() throws Exception {
        String token = signUpWithProfile("quiz1@khu.ac.kr", "퀴즈1");
        JsonNode questions = fetchQuestions(token);

        // 시드 문항 23개 (seed/content_meta.json: COURSE_REG 6 + TRANSPORT 5 + FOOD 4 + CAMPUS_SITE 4 + HUMANITIES 4)
        assertThat(questions).hasSize(23);
        JsonNode first = questions.get(0);
        assertThat(first.has("answerIndex")).isFalse();
        assertThat(first.has("answer")).isFalse();
        assertThat(first.get("options").isArray()).isTrue();
        assertThat(first.get("category").asText()).isNotBlank();
    }

    @Test
    @DisplayName("category 필터: 'COURSE_REG' 카테고리만 반환 (시드상 6문항)")
    void getQuestionsByCategory() throws Exception {
        String token = signUpWithProfile("quiz2@khu.ac.kr", "퀴즈2");
        MvcResult result = mockMvc.perform(get("/api/quiz/questions")
                        .param("category", "COURSE_REG")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode questions = readData(result);
        assertThat(questions).hasSize(6);
        for (JsonNode q : questions) {
            assertThat(q.get("category").asText()).isEqualTo("COURSE_REG");
        }
    }

    @Test
    @DisplayName("category는 BadgeId enum 닫힌 집합 — 알 수 없는 값(옛 한글 키 등)은 빈 목록이 아니라 400으로 거부된다")
    void unknownCategoryRejected() throws Exception {
        String token = signUpWithProfile("quiz6@khu.ac.kr", "퀴즈6");
        // 운영 사고를 일으킨 옛 한글 키. 이제는 조용히 사라지지 않고 명확히 400.
        mockMvc.perform(get("/api/quiz/questions")
                        .param("category", "수강신청")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("전부 정답 제출: score=100.0, profiles.quiz_score 갱신, results/me·score/me 반영")
    void submitAllCorrect() throws Exception {
        String token = signUpWithProfile("quiz3@khu.ac.kr", "퀴즈3");
        long memberId = myMemberId(token);
        Map<Long, Integer> answers = seededAnswers();

        List<Map<String, Object>> items = new ArrayList<>();
        answers.forEach((qid, idx) -> items.add(Map.of("questionId", qid, "selectedOption", idx)));

        mockMvc.perform(post("/api/quiz/submit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", items))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(answers.size()))
                .andExpect(jsonPath("$.data.totalCount").value(answers.size()))
                .andExpect(jsonPath("$.data.score").value(100.0))
                .andExpect(jsonPath("$.data.results[0].correct").value(true));

        // profiles.quiz_score 갱신 확인
        Double quizScore = jdbcTemplate.queryForObject(
                "SELECT quiz_score FROM profiles WHERE member_id = ?", Double.class, memberId);
        assertThat(quizScore).isEqualTo(100.0);

        mockMvc.perform(get("/api/quiz/score/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data").value(100.0));

        mockMvc.perform(get("/api/quiz/results/me").header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data[0].score").value(100.0))
                .andExpect(jsonPath("$.data[0].correctCount").value(answers.size()));
    }

    @Test
    @DisplayName("부분 정답 제출: 점수는 (정답수/총문항)*100 소수1자리 반올림")
    void submitPartial() throws Exception {
        String token = signUpWithProfile("quiz4@khu.ac.kr", "퀴즈4");
        Map<Long, Integer> answers = seededAnswers();

        // 첫 3문항만 제출, 그 중 첫 문항만 정답 처리
        List<Long> ids = new ArrayList<>(answers.keySet());
        ids.sort(Long::compareTo);
        List<Map<String, Object>> items = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            long qid = ids.get(i);
            int correct = answers.get(qid);
            int chosen = (i == 0) ? correct : (correct + 1) % 4; // 0번만 정답
            items.add(Map.of("questionId", qid, "selectedOption", chosen));
        }

        // 1/3 정답 → 33.3
        mockMvc.perform(post("/api/quiz/submit")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("answers", items))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.correctCount").value(1))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.score").value(33.3));
    }

    @Test
    @DisplayName("현재 동작: 3회 도전 제한이 코드에 없음 — 4회 연속 제출도 모두 200, 히스토리에 4건 누적")
    void noAttemptLimit() throws Exception {
        String token = signUpWithProfile("quiz5@khu.ac.kr", "퀴즈5");
        Map<Long, Integer> answers = seededAnswers();
        long firstQid = answers.keySet().iterator().next();
        int firstIdx = answers.get(firstQid);
        String body = objectMapper.writeValueAsString(Map.of(
                "answers", List.of(Map.of("questionId", firstQid, "selectedOption", firstIdx))));

        // 문서상 "3회 도전 제한"이라 적혀 있으나 실제 백엔드엔 제한 로직이 없다.
        for (int i = 0; i < 4; i++) {
            mockMvc.perform(post("/api/quiz/submit")
                            .header("Authorization", bearer(token))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/quiz/results/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(4));
    }
}
