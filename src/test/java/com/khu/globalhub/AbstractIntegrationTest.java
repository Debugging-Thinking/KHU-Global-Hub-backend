package com.khu.globalhub;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khu.globalhub.domain.auth.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 모든 통합테스트의 베이스. 실 PostgreSQL 컨테이너를 띄워 운영과 동일 DB로 검증한다.
 *
 * <p>BC 격리 리팩토링의 안전망 — 리팩토링 전 현재 동작을 characterization test로 박제하고,
 * 리팩토링 후에도 동일하게 통과하는지로 "행동 불변"을 보장한다.
 *
 * <p>{@link ServiceConnection}이 컨테이너 접속정보를 datasource로 자동 주입한다.
 * 외부 의존(이메일)은 목 처리하고, 매 테스트 전 DB를 비운다(퀴즈 시드 제외).
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    // 싱글톤 컨테이너: JVM당 한 번만 시작하고 멈추지 않는다.
    // (@Testcontainers 클래스별 생명주기를 쓰면 컨테이너가 클래스마다 재시작되고,
    //  Spring이 캐시한 컨텍스트가 멈춘 옛 포트를 붙들어 연결 실패(CannotGetJdbcConnection)가 난다.
    //  그래서 수동 싱글톤으로 띄운다. JVM 종료 시 Testcontainers Ryuk가 정리.)
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    static {
        POSTGRES.start();
    }

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    /** 이메일 발송은 목 처리 — 발송 대신 인증 코드를 캡처해서 테스트에서 사용한다. */
    @MockitoBean protected EmailService emailService;

    @BeforeEach
    void resetState() {
        cleanDatabase();
        Mockito.reset(emailService);
    }

    /** 퀴즈 시드/플라이웨이 이력을 제외한 모든 테이블을 비우고 ID 시퀀스를 초기화한다. */
    private void cleanDatabase() {
        List<String> tables = jdbcTemplate.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
                        "AND tablename NOT IN ('flyway_schema_history', 'quiz_questions', 'quiz_options')",
                String.class);
        if (tables.isEmpty()) {
            return;
        }
        jdbcTemplate.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
    }

    // ── 인증 헬퍼 ──────────────────────────────────────────────

    /** 회원가입 + 이메일 인증까지 마친 계정의 accessToken을 반환 (프로필 없음). */
    protected String signUp(String email) throws Exception {
        return signUp(email, "password123").accessToken();
    }

    /** 회원가입 + 이메일 인증 후 발급된 토큰 묶음을 반환. */
    protected Tokens signUp(String email, String password) throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "password", password)))
                .andExpect(status().isOk());

        // 목으로 가로챈 인증 코드 캡처
        ArgumentCaptor<String> codeCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendVerificationEmail(eq(email), codeCaptor.capture());
        String code = codeCaptor.getValue();

        MvcResult result = mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", email, "code", code)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode data = readData(result);
        return new Tokens(data.get("accessToken").asText(),
                data.get("refreshToken").asText(),
                data.get("hasProfile").asBoolean());
    }

    /** 회원가입 + 인증 + 프로필 생성까지 마친 계정의 accessToken을 반환. */
    protected String signUpWithProfile(String email, String name) throws Exception {
        String token = signUp(email, "password123").accessToken();
        mockMvc.perform(post("/api/auth/profile")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of(
                                "name", name,
                                "department", "컴퓨터공학과",
                                "nationality", "대한민국",
                                "admissionYear", 2020,
                                "language", "KO",
                                "mentoringRole", "MENTEE"))))
                .andExpect(status().isOk());
        return token;
    }

    // ── 유틸 ──────────────────────────────────────────────────

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** ("k1", v1, "k2", v2, ...) → JSON 문자열. */
    protected String json(Object... kv) throws Exception {
        var map = new java.util.LinkedHashMap<String, Object>();
        for (int i = 0; i < kv.length; i += 2) {
            map.put((String) kv[i], kv[i + 1]);
        }
        return objectMapper.writeValueAsString(map);
    }

    /** ApiResponse 응답에서 data 노드를 추출. */
    protected JsonNode readData(MvcResult result) throws Exception {
        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        return root.get("data");
    }

    /** 인증 후 발급되는 토큰 묶음. */
    protected record Tokens(String accessToken, String refreshToken, boolean hasProfile) {}
}
