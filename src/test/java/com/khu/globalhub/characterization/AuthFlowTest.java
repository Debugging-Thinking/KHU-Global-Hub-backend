package com.khu.globalhub.characterization;

import com.fasterxml.jackson.databind.JsonNode;
import com.khu.globalhub.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 인증 플로우 현재 동작 박제 (characterization).
 * BC 리팩토링(identity/profile 분리) 후에도 이 동작이 동일하게 유지되어야 한다.
 */
@DisplayName("[characterization] 인증 플로우")
class AuthFlowTest extends AbstractIntegrationTest {

    @Test
    @DisplayName("회원가입→이메일 인증 시 토큰 발급, 프로필 미생성이라 hasProfile=false")
    void registerThenVerify() throws Exception {
        Tokens tokens = signUp("alice@khu.ac.kr", "password123");

        assertThat(tokens.accessToken()).isNotBlank();
        assertThat(tokens.refreshToken()).isNotBlank();
        assertThat(tokens.hasProfile()).isFalse();
    }

    @Test
    @DisplayName("@khu.ac.kr 이 아닌 이메일은 400 INVALID_EMAIL_DOMAIN")
    void registerRejectsNonKhuEmail() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "bob@gmail.com", "password", "password123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("@khu.ac.kr 이메일만 허용됩니다."));
    }

    @Test
    @DisplayName("이미 가입된 이메일로 재가입 시 409 EMAIL_ALREADY_EXISTS")
    void registerRejectsDuplicateEmail() throws Exception {
        signUp("carol@khu.ac.kr", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "carol@khu.ac.kr", "password", "password123")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("틀린 인증 코드는 400 INVALID_VERIFICATION_CODE")
    void verifyRejectsWrongCode() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "dave@khu.ac.kr", "password", "password123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "dave@khu.ac.kr", "code", "000000")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("프로필 생성 후 로그인하면 hasProfile=true")
    void loginReflectsProfileExistence() throws Exception {
        signUpWithProfile("erin@khu.ac.kr", "에린");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "erin@khu.ac.kr", "password", "password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.hasProfile").value(true))
                .andReturn();

        JsonNode data = readData(result);
        assertThat(data.get("accessToken").asText()).isNotBlank();
    }

    @Test
    @DisplayName("신입생(올해 입학)이 MENTOR 선택 시 400 INVALID_MENTORING_ROLE")
    void newStudentCannotBeMentor() throws Exception {
        String token = signUp("frank@khu.ac.kr");
        int thisYear = java.time.LocalDate.now().getYear();

        mockMvc.perform(post("/api/auth/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", "프랭크",
                                "department", "컴퓨터공학과",
                                "nationality", "대한민국",
                                "admissionYear", thisYear,
                                "language", "KO",
                                "mentoringRole", "MENTOR"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("신입생은 멘티만 선택 가능합니다."));
    }

    @Test
    @DisplayName("비밀번호 재설정: forgot→reset 후 새 비번으로 로그인, 기존 refreshToken 무효화")
    void passwordResetFlow() throws Exception {
        Tokens before = signUp("grace@khu.ac.kr", "oldpassword1");

        // forgot-password → 재설정 코드 발송 (목 캡처)
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "grace@khu.ac.kr")))
                .andExpect(status().isOk());

        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("grace@khu.ac.kr"), codeCaptor.capture());
        String resetCode = codeCaptor.getValue();

        // reset-password
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "grace@khu.ac.kr", "code", resetCode, "newPassword", "newpassword1")))
                .andExpect(status().isOk());

        // 기존 refreshToken은 즉시 무효화 — 재로그인 전에 검증
        // (재로그인하면 같은 초에 동일 JWT가 재발급될 수 있어 검증 순서를 분리)
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("refreshToken", before.refreshToken())))
                .andExpect(status().isUnauthorized());

        // 새 비번으로 로그인 성공
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", "grace@khu.ac.kr", "password", "newpassword1")))
                .andExpect(status().isOk());
    }
}
