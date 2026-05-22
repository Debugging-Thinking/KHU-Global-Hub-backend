package com.khu.globalhub.characterization;

import com.fasterxml.jackson.databind.JsonNode;
import com.khu.globalhub.AbstractIntegrationTest;
import com.khu.globalhub.shared.infra.S3Service;
import com.khu.globalhub.shared.infra.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 게시판(/api/posts) + 댓글 플로우 현재 동작 박제 (characterization).
 *
 * <p>번역(@Async)·S3 업로드는 외부 I/O라 목 처리한다. 원문 번역 행은 동기 저장되므로
 * KO 조회 시 항상 원문이 반환된다.
 */
@DisplayName("[characterization] 게시판/댓글 플로우")
class BoardFlowTest extends AbstractIntegrationTest {

    @MockitoBean
    TranslationService translationService;
    @MockitoBean
    S3Service s3Service;

    /** 멀티파트 게시글 작성 후 생성된 postId 반환 (텍스트 전용). */
    private long createPost(String token, String title, String content, boolean anonymous) throws Exception {
        MvcResult result = mockMvc.perform(multipart("/api/posts")
                        .param("title", title)
                        .param("content", content)
                        .param("isAnonymous", String.valueOf(anonymous))
                        .param("language", "KO")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).asLong();
    }

    @Test
    @DisplayName("게시글 작성(멀티파트)→상세 조회: 원문 KO 반환, originalLanguage=KO, isOwner=true")
    void createAndGetDetail() throws Exception {
        String token = signUpWithProfile("poster@khu.ac.kr", "포스터");
        long postId = createPost(token, "제목입니다", "본문입니다", false);

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .param("language", "KO")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("제목입니다"))
                .andExpect(jsonPath("$.data.content").value("본문입니다"))
                .andExpect(jsonPath("$.data.authorName").value("포스터"))
                .andExpect(jsonPath("$.data.isAnonymous").value(false))
                .andExpect(jsonPath("$.data.isOwner").value(true))
                .andExpect(jsonPath("$.data.isLiked").value(false))
                .andExpect(jsonPath("$.data.commentCount").value(0))
                .andExpect(jsonPath("$.data.originalLanguage").value("KO"))
                .andExpect(jsonPath("$.data.imageUrls").isArray());
    }

    @Test
    @DisplayName("이미지 없이 작성해도 S3Service.uploadPostImages는 호출되지 않는다")
    void textOnlyPostSkipsS3() throws Exception {
        String token = signUpWithProfile("noimg@khu.ac.kr", "노이미지");
        createPost(token, "t", "c", false);
        // images=null → 업로드 분기 미진입
        org.mockito.Mockito.verifyNoInteractions(s3Service);
    }

    @Test
    @DisplayName("게시판 목록은 페이징 응답(content/totalElements) 형태로 반환")
    void listWithPaging() throws Exception {
        String token = signUpWithProfile("lister@khu.ac.kr", "리스터");
        createPost(token, "first", "c1", false);
        createPost(token, "second", "c2", false);

        mockMvc.perform(get("/api/posts")
                        .param("language", "KO")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content").isArray())
                // createdAt DESC 정렬: 나중에 쓴 글이 먼저
                .andExpect(jsonPath("$.data.content[0].title").value("second"));
    }

    @Test
    @DisplayName("좋아요 토글: 첫 호출 true(좋아요), 두 번째 false(취소), likeCount 반영")
    void likeToggle() throws Exception {
        String token = signUpWithProfile("liker@khu.ac.kr", "라이커");
        long postId = createPost(token, "t", "c", false);

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.likeCount").value(1))
                .andExpect(jsonPath("$.data.isLiked").value(true));

        mockMvc.perform(post("/api/posts/{postId}/like", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data").value(false));

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.isLiked").value(false));
    }

    @Test
    @DisplayName("게시글 삭제는 작성자만 가능 — 타인은 403 POST_UNAUTHORIZED")
    void deleteAuthorOnly() throws Exception {
        String author = signUpWithProfile("owner@khu.ac.kr", "주인");
        String other = signUpWithProfile("intruder@khu.ac.kr", "침입자");
        long postId = createPost(author, "t", "c", false);

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("게시글을 수정/삭제할 권한이 없습니다."));

        mockMvc.perform(delete("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(author)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(author)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("댓글 작성→목록 조회: 작성자 실명, commentCount 1 증가")
    void commentCreateAndList() throws Exception {
        String token = signUpWithProfile("commenter@khu.ac.kr", "댓글러");
        long postId = createPost(token, "t", "c", false);

        MvcResult created = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", "첫 댓글")))
                .andExpect(status().isOk())
                .andReturn();
        long commentId = readData(created).asLong();
        assertThat(commentId).isPositive();

        mockMvc.perform(get("/api/posts/{postId}/comments", postId)
                        .param("language", "KO")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].content").value("첫 댓글"))
                .andExpect(jsonPath("$.data[0].authorName").value("댓글러"))
                .andExpect(jsonPath("$.data[0].isOwner").value(true));

        // commentCount 비정규화 반영
        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.commentCount").value(1));
    }

    @Test
    @DisplayName("댓글 좋아요 토글 + 댓글 삭제(작성자만), 삭제 시 commentCount 감소")
    void commentLikeAndDelete() throws Exception {
        String token = signUpWithProfile("cl@khu.ac.kr", "씨엘");
        long postId = createPost(token, "t", "c", false);
        MvcResult created = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", "지울 댓글")))
                .andReturn();
        long commentId = readData(created).asLong();

        mockMvc.perform(post("/api/comments/{id}/like", commentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        mockMvc.perform(get("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data[0].likeCount").value(1))
                .andExpect(jsonPath("$.data[0].isLiked").value(true));

        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.commentCount").value(0));
    }

    @Test
    @DisplayName("타인 댓글은 삭제 불가 — 403 COMMENT_UNAUTHORIZED")
    void commentDeleteOtherForbidden() throws Exception {
        String author = signUpWithProfile("ca@khu.ac.kr", "씨에이");
        String other = signUpWithProfile("cb@khu.ac.kr", "씨비");
        long postId = createPost(author, "t", "c", false);
        MvcResult created = mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", false, "language", "KO", "content", "내 댓글")))
                .andReturn();
        long commentId = readData(created).asLong();

        mockMvc.perform(delete("/api/comments/{id}", commentId)
                        .header("Authorization", bearer(other)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("댓글을 수정/삭제할 권한이 없습니다."));
    }

    @Test
    @DisplayName("익명 게시글 작성자는 익명1, 이후 익명 댓글 작성자는 익명2; 같은 사람은 같은 컨텍스트에서 동일 번호 유지")
    void anonymousNumbering() throws Exception {
        String author = signUpWithProfile("anonA@khu.ac.kr", "에이");
        String commenterB = signUpWithProfile("anonB@khu.ac.kr", "비");

        long postId = createPost(author, "비밀글", "익명 본문", true);

        // 작성자(익명) = 익명1
        mockMvc.perform(get("/api/posts/{postId}", postId)
                        .header("Authorization", bearer(author)))
                .andExpect(jsonPath("$.data.authorName").value("익명1"))
                // 익명이면 authorId는 null
                .andExpect(jsonPath("$.data.authorId").doesNotExist());

        // B가 익명 댓글 → 익명2
        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(commenterB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", true, "language", "KO", "content", "B의 익명 댓글1")))
                .andExpect(status().isOk());
        // 같은 B가 다시 익명 댓글 → 여전히 익명2
        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(commenterB))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", true, "language", "KO", "content", "B의 익명 댓글2")))
                .andExpect(status().isOk());
        // 작성자 A도 익명 댓글 → 원래 본인 번호 익명1 유지
        mockMvc.perform(post("/api/posts/{postId}/comments", postId)
                        .header("Authorization", bearer(author))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("isAnonymous", true, "language", "KO", "content", "A의 익명 댓글")))
                .andExpect(status().isOk());

        MvcResult list = mockMvc.perform(get("/api/posts/{postId}/comments", postId)
                        .param("language", "KO")
                        .header("Authorization", bearer(author)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode comments = readData(list);
        // 정렬: createdAt ASC → [B1, B2, A]
        assertThat(comments.get(0).get("authorName").asText()).isEqualTo("익명2");
        assertThat(comments.get(1).get("authorName").asText()).isEqualTo("익명2");
        assertThat(comments.get(2).get("authorName").asText()).isEqualTo("익명1");
        // 익명 댓글의 authorId는 null
        assertThat(comments.get(0).get("authorId").isNull()).isTrue();
    }
}
