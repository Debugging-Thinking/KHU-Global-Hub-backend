package com.khu.globalhub.characterization;

import com.khu.globalhub.AbstractIntegrationTest;
import com.khu.globalhub.global.infra.S3Service;
import com.khu.globalhub.global.infra.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Q&A(/api/qnas) 플로우 현재 동작 박제 (characterization).
 * 번역(@Async)은 외부 I/O라 목 처리. 원문 행은 동기 저장된다.
 */
@DisplayName("[characterization] Q&A 플로우")
class QnaFlowTest extends AbstractIntegrationTest {

    // 모든 characterization 클래스가 동일한 @MockitoBean 셋을 선언해 하나의 Spring 컨텍스트(=단일 컨테이너)를 공유한다.
    @MockitoBean
    TranslationService translationService;
    @MockitoBean
    S3Service s3Service;

    private long createQna(String token, String title, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/qnas")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "title", title, "content", content)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).asLong();
    }

    private long createAnswer(String token, long qnaId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/qnas/{qnaId}/answers", qnaId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", content)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).asLong();
    }

    @Test
    @DisplayName("질문 작성→목록→상세: 원문 KO 반환, originalLanguage=KO, isAdopted=false")
    void createListDetail() throws Exception {
        String asker = signUpWithProfile("asker@khu.ac.kr", "질문자");
        long qnaId = createQna(asker, "질문 제목", "질문 본문");

        mockMvc.perform(get("/api/qnas").param("language", "KO")
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("질문 제목"));

        mockMvc.perform(get("/api/qnas/{qnaId}", qnaId).param("language", "KO")
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("질문 제목"))
                .andExpect(jsonPath("$.data.content").value("질문 본문"))
                .andExpect(jsonPath("$.data.authorName").value("질문자"))
                .andExpect(jsonPath("$.data.isAdopted").value(false))
                .andExpect(jsonPath("$.data.isOwner").value(true))
                .andExpect(jsonPath("$.data.originalLanguage").value("KO"))
                .andExpect(jsonPath("$.data.answers").isArray());
    }

    @Test
    @DisplayName("답변 작성→상세에 답변 노출")
    void answerAppearsInDetail() throws Exception {
        String asker = signUpWithProfile("a1@khu.ac.kr", "질문A");
        String responder = signUpWithProfile("r1@khu.ac.kr", "답변A");
        long qnaId = createQna(asker, "Q", "C");
        createAnswer(responder, qnaId, "내 답변");

        mockMvc.perform(get("/api/qnas/{qnaId}", qnaId)
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.answers[0].content").value("내 답변"))
                .andExpect(jsonPath("$.data.answers[0].authorName").value("답변A"))
                .andExpect(jsonPath("$.data.answers[0].isAdopted").value(false));
    }

    @Test
    @DisplayName("본인 질문에는 답변 불가 — 400 SELF_ANSWER_NOT_ALLOWED")
    void selfAnswerNotAllowed() throws Exception {
        String asker = signUpWithProfile("self@khu.ac.kr", "셀프");
        long qnaId = createQna(asker, "Q", "C");

        mockMvc.perform(post("/api/qnas/{qnaId}/answers", qnaId)
                        .header("Authorization", bearer(asker))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", "내 질문에 답변")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("본인의 질문에는 답변할 수 없습니다."));
    }

    @Test
    @DisplayName("1인 1답 제한 — 두 번째 답변은 409 ANSWER_ALREADY_EXISTS")
    void oneAnswerPerAccount() throws Exception {
        String asker = signUpWithProfile("a2@khu.ac.kr", "질문2");
        String responder = signUpWithProfile("r2@khu.ac.kr", "답변2");
        long qnaId = createQna(asker, "Q", "C");
        createAnswer(responder, qnaId, "첫 답변");

        mockMvc.perform(post("/api/qnas/{qnaId}/answers", qnaId)
                        .header("Authorization", bearer(responder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", "두 번째 답변")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 이 질문에 답변하셨습니다."));
    }

    @Test
    @DisplayName("채택: 질문 작성자만 가능 — 답변자가 채택 시도하면 403 ADOPT_UNAUTHORIZED")
    void adoptUnauthorized() throws Exception {
        String asker = signUpWithProfile("a3@khu.ac.kr", "질문3");
        String responder = signUpWithProfile("r3@khu.ac.kr", "답변3");
        long qnaId = createQna(asker, "Q", "C");
        long answerId = createAnswer(responder, qnaId, "답변");

        mockMvc.perform(post("/api/qnas/{qnaId}/answers/{answerId}/adopt", qnaId, answerId)
                        .header("Authorization", bearer(responder)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("채택 권한이 없습니다."));
    }

    @Test
    @DisplayName("채택 성공 → 답변/질문 isAdopted=true, 재채택 시 409 ALREADY_ADOPTED, 채택 후 답변 추가는 409 QNA_ALREADY_ADOPTED")
    void adoptThenRulesAfterAdoption() throws Exception {
        String asker = signUpWithProfile("a4@khu.ac.kr", "질문4");
        String responder = signUpWithProfile("r4@khu.ac.kr", "답변4");
        String responder2 = signUpWithProfile("r4b@khu.ac.kr", "답변4b");
        long qnaId = createQna(asker, "Q", "C");
        long answerId = createAnswer(responder, qnaId, "답변");

        mockMvc.perform(post("/api/qnas/{qnaId}/answers/{answerId}/adopt", qnaId, answerId)
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/qnas/{qnaId}", qnaId)
                        .header("Authorization", bearer(asker)))
                .andExpect(jsonPath("$.data.isAdopted").value(true))
                .andExpect(jsonPath("$.data.answers[0].isAdopted").value(true));

        // 재채택 시도 → 409 ALREADY_ADOPTED
        mockMvc.perform(post("/api/qnas/{qnaId}/answers/{answerId}/adopt", qnaId, answerId)
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("이미 채택된 답변이 있습니다."));

        // 채택 완료 질문엔 답변 추가 불가 → 409 QNA_ALREADY_ADOPTED
        mockMvc.perform(post("/api/qnas/{qnaId}/answers", qnaId)
                        .header("Authorization", bearer(responder2))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", "늦은 답변")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("채택이 완료된 질문에는 더 이상 답변할 수 없습니다."));
    }

    @Test
    @DisplayName("질문 좋아요 토글 + 답변 좋아요 토글")
    void likesOnQuestionAndAnswer() throws Exception {
        String asker = signUpWithProfile("a5@khu.ac.kr", "질문5");
        String responder = signUpWithProfile("r5@khu.ac.kr", "답변5");
        long qnaId = createQna(asker, "Q", "C");
        long answerId = createAnswer(responder, qnaId, "답변");

        mockMvc.perform(post("/api/qnas/{qnaId}/like", qnaId)
                        .header("Authorization", bearer(responder)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(post("/api/qnas/{qnaId}/answers/{answerId}/like", qnaId, answerId)
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/qnas/{qnaId}", qnaId)
                        .header("Authorization", bearer(asker)))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                // 질문 좋아요는 responder가 눌렀고 조회자는 asker → asker 시점 isLiked=false
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.answers[0].likeCount").value(1))
                // 답변 좋아요는 asker가 눌렀으므로 asker 시점 isLiked=true
                .andExpect(jsonPath("$.data.answers[0].isLiked").value(true));

        // 답변 좋아요 취소
        mockMvc.perform(post("/api/qnas/{qnaId}/answers/{answerId}/like", qnaId, answerId)
                        .header("Authorization", bearer(asker)))
                .andExpect(jsonPath("$.data").value(false));
    }

    @Test
    @DisplayName("질문 삭제는 작성자만 — 타인은 403 QNA_UNAUTHORIZED, 작성자는 답변 cascade 삭제")
    void deleteQuestionAuthorOnly() throws Exception {
        String asker = signUpWithProfile("a6@khu.ac.kr", "질문6");
        String other = signUpWithProfile("o6@khu.ac.kr", "타인6");
        long qnaId = createQna(asker, "Q", "C");
        createAnswer(other, qnaId, "답변");

        mockMvc.perform(delete("/api/qnas/{qnaId}", qnaId)
                        .header("Authorization", bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Q&A를 수정/삭제할 권한이 없습니다."));

        mockMvc.perform(delete("/api/qnas/{qnaId}", qnaId)
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/qnas/{qnaId}", qnaId)
                        .header("Authorization", bearer(asker)))
                .andExpect(status().isNotFound());
    }
}
