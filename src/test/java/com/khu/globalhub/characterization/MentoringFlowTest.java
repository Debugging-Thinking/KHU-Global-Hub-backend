package com.khu.globalhub.characterization;

import com.khu.globalhub.AbstractIntegrationTest;
import com.khu.globalhub.global.infra.S3Service;
import com.khu.globalhub.global.infra.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 멘토링(/api/mentoring/me) 플로우 현재 동작 박제 (characterization).
 * 매칭 생성은 스케줄러 전용이라, 채워진 케이스는 JdbcTemplate로 match 행을 직접 삽입해 검증한다.
 */
@DisplayName("[characterization] 멘토링 플로우")
class MentoringFlowTest extends AbstractIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    // 모든 characterization 클래스가 동일한 @MockitoBean 셋을 선언해 단일 Spring 컨텍스트(=단일 컨테이너)를 공유한다.
    @MockitoBean
    TranslationService translationService;
    @MockitoBean
    S3Service s3Service;

    /** GET /api/members/me 로 내 memberId 조회. */
    private long myMemberId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/members/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).get("memberId").asLong();
    }

    @Test
    @DisplayName("ACTIVE 매칭이 없으면 404 MATCH_NOT_FOUND")
    void noMatchReturns404() throws Exception {
        String token = signUpWithProfile("lonely@khu.ac.kr", "혼자");

        mockMvc.perform(get("/api/mentoring/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("현재 진행 중인 매칭이 없습니다."));
    }

    @Test
    @DisplayName("ACTIVE 매칭이 있으면 내 역할/상대 프로필 포함 응답 — 멘티 시점에서 myRole=MENTEE, partner=멘토")
    void activeMatchReturnsPartner() throws Exception {
        String mentor = signUpWithProfile("mentor@khu.ac.kr", "멘토님");
        String mentee = signUpWithProfile("mentee@khu.ac.kr", "멘티님");
        long mentorId = myMemberId(mentor);
        long menteeId = myMemberId(mentee);

        jdbcTemplate.update(
                "INSERT INTO mentor_mentee_matches (mentor_id, mentee_id, semester, status, matched_at) " +
                        "VALUES (?, ?, ?, ?, now())",
                mentorId, menteeId, "2025-1", "ACTIVE");

        // 멘티 시점
        mockMvc.perform(get("/api/mentoring/me")
                        .header("Authorization", bearer(mentee)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.semester").value("2025-1"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.myRole").value("MENTEE"))
                .andExpect(jsonPath("$.data.partner.name").value("멘토님"));

        // 멘토 시점
        mockMvc.perform(get("/api/mentoring/me")
                        .header("Authorization", bearer(mentor)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.myRole").value("MENTOR"))
                .andExpect(jsonPath("$.data.partner.name").value("멘티님"));
    }
}
