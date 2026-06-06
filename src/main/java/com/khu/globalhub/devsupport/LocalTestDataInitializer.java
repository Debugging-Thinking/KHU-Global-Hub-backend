package com.khu.globalhub.devsupport;

import com.khu.globalhub.shared.infra.AzureTranslateClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 로컬 개발용 테스트 데이터 시드 (local 프로필 전용).
 *
 * <p>로컬은 SMTP가 더미라 회원가입(이메일 인증)이 안 되므로, 팀원이 매번 계정을 만들 필요 없이
 * 표준 테스트 계정과 샘플 콘텐츠를 자동 생성한다. 모든 계정 비밀번호는 {@code password123}.
 *
 * <p><b>매 실행마다 초기화 후 재시드</b> — 켤 때마다 항상 동일한 테스트 셋이 보장된다
 * (이전 실행에서 만지작거린 데이터가 남지 않음). quiz 문항/Flyway 이력은 보존.
 *
 * <ul>
 *   <li>{@code @Profile("local")} — 운영(prod)에서는 절대 실행되지 않는다.</li>
 *   <li>datasource가 localhost가 아니면 건드리지 않는다(원격 DB 안전장치).</li>
 *   <li>JdbcTemplate만 사용 — 특정 BC 패키지에 의존하지 않는 순수 개발 보조 도구.</li>
 * </ul>
 *
 * 계정: demo / alice / bob / carol @khu.ac.kr
 */
@Slf4j
@Component
@Profile({"local", "prod"})
@RequiredArgsConstructor
public class LocalTestDataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final AzureTranslateClient azureClient;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    /** 시드 원문은 KO. 나머지 5개 언어로 번역해 함께 저장한다(키 없으면 KO만). */
    private static final List<String> OTHER_LANGS = List.of("EN", "ZH", "VI", "UZ", "MN");
    private static final List<String> OTHER_CODES = List.of("en", "zh-Hans", "vi", "uz", "mn-Cyrl");

    @Override
    public void run(ApplicationArguments args) {
        boolean localDb = datasourceUrl != null
                && (datasourceUrl.contains("localhost") || datasourceUrl.contains("127.0.0.1"));

        if (localDb) {
            // 로컬: 매 실행마다 동일 셋 보장 — 초기화 후 재시드 (quiz/Flyway/강의 보존).
            resetTestData();
            log.info("[Seed] 로컬 테스트 데이터 초기화 후 재시드…");
        } else {
            // 운영(prod): truncate 절대 금지. 데모 데이터가 없을 때(빈 DB)만 1회 시드해 기존 데이터를 보존.
            Integer members = jdbc.queryForObject("SELECT count(*) FROM members", Integer.class);
            if (members != null && members > 0) {
                log.info("[Seed] 기존 데이터({}명) 존재 → 운영 데모 시드 건너뜀(데이터 보존)", members);
                return;
            }
            log.info("[Seed] 운영 DB 비어있음 → 데모 데이터 1회 시드(truncate 없음)…");
        }

        String pw = passwordEncoder.encode("password123");

        // department/nationality는 프론트 드롭다운과 동일하게 "코드"로 저장 (CSE=컴퓨터공학과, KR=대한민국 …) → 보는 사람 언어로 현지화됨
        long demo = member("demo@khu.ac.kr", pw);   profile(demo, "데모", "CSE", "KR", 2020, "MENTEE");
        long alice = member("alice@khu.ac.kr", pw);  profile(alice, "앨리스", "GBA", "US", 2024, "MENTEE");
        long bob = member("bob@khu.ac.kr", pw);      profile(bob, "밥", "EE", "CA", 2021, "MENTOR");
        long carol = member("carol@khu.ac.kr", pw);  profile(carol, "캐롤", "IS", "VN", 2023, "MENTEE");

        // 게시글 (서로 다른 작성자 → 남의 글에 댓글 테스트 가능)
        long pAlice = post(alice, "기숙사 룸메 구해요 🛏", "이번 학기 같이 지낼 룸메 구합니다. 깔끔하신 분!", 3);
        long pBob = post(bob, "졸업 준비 체크리스트 공유", "졸업요건/영어성적/포트폴리오 정리해봤어요.", 7);
        post(carol, "학교 근처 맛집 추천 받아요 🍜", "혼밥하기 좋은 곳 있을까요?", 2);

        // 남의 글에 달린 댓글 (demo가 자기 댓글 추가해서 테스트할 수 있게 미리 1개씩)
        comment(pAlice, bob, "저 관심있어요! 쪽지 드릴게요");
        comment(pBob, carol, "오 감사합니다 도움됐어요 🙏");
        bumpCommentCount(pAlice, 1);
        bumpCommentCount(pBob, 1);

        // Q&A + 답변 (채택 테스트용)
        long q1 = qna(alice, "수강신청 꿀팁 있나요?", "장바구니 담아두면 유리한가요?");
        answer(q1, bob, "장바구니는 필수예요. 그리고 매크로 말고 미리 로그인 해두세요!");

        // 채팅 (demo ↔ alice 대화 — 채팅 목록/상세 테스트)
        chat(demo, alice, "안녕하세요! 게시글 보고 연락드려요 :)", true);
        chat(alice, demo, "네 안녕하세요~ 어떤 거 궁금하세요?", false);
        chat(demo, alice, "룸메 아직 구하시나요?", false);

        // 멘토링 매칭 (bob 멘토 ↔ demo 멘티 — /api/mentoring/me 테스트)
        match(bob, demo, "2026-1");

        // 강의(lectures)/강의평은 데모 시드가 아님 — KHU 종합시간표 스크래퍼가 채우는 영속 참조 데이터.
        // resetTestData()에서 제외되어 재시작에도 보존된다. (가짜 강의 시드 제거됨)

        log.info("[LocalSeed] 완료 ✅  로그인 계정: demo / alice / bob / carol @khu.ac.kr  (비밀번호: password123)");
    }

    /**
     * 데모 시드 테이블만 비우고 ID 시퀀스를 초기화한다.
     * 제외: Flyway 이력, quiz 문항(고정), 그리고 **강의(lectures)/강의평** — 실데이터(스크래퍼)라 보존.
     */
    private void resetTestData() {
        List<String> tables = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public' " +
                        "AND tablename NOT IN ('flyway_schema_history', 'quiz_questions', 'quiz_options', " +
                        "'lectures', 'course_reviews', 'course_review_translations')",
                String.class);
        if (!tables.isEmpty()) {
            jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
        }
    }

    private long member(String email, String encodedPw) {
        return jdbc.queryForObject(
                "INSERT INTO members(email, password, is_email_verified, created_at, updated_at) " +
                        "VALUES (?, ?, true, now(), now()) RETURNING id",
                Long.class, email, encodedPw);
    }

    private void profile(long memberId, String name, String dept, String nationality, int year, String role) {
        jdbc.update(
                "INSERT INTO profiles(member_id, name, department, nationality, admission_year, language, " +
                        "mentoring_role, quiz_score, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, 'KO', ?, 0.0, now(), now())",
                memberId, name, dept, nationality, year, role);
    }

    private long post(long authorId, String title, String content, int likes) {
        Long id = jdbc.queryForObject(
                "INSERT INTO posts(author_id, is_anonymous, like_count, comment_count, created_at, updated_at) " +
                        "VALUES (?, false, ?, 0, now(), now()) RETURNING id",
                Long.class, authorId, likes);
        seedTranslations("post_translations", "post_id", id, title, content);
        return id;
    }

    private void bumpCommentCount(long postId, int by) {
        jdbc.update("UPDATE posts SET comment_count = comment_count + ? WHERE id = ?", by, postId);
    }

    private void comment(long postId, long authorId, String content) {
        Long id = jdbc.queryForObject(
                "INSERT INTO comments(target_id, parent_id, author_id, is_anonymous, like_count, created_at, updated_at) " +
                        "VALUES (?, NULL, ?, false, 0, now(), now()) RETURNING id",
                Long.class, postId, authorId);
        seedTranslations("comment_translations", "comment_id", id, null, content);
    }

    private long qna(long authorId, String title, String content) {
        Long id = jdbc.queryForObject(
                "INSERT INTO qnas(author_id, is_anonymous, is_adopted, like_count, created_at, updated_at) " +
                        "VALUES (?, false, false, 0, now(), now()) RETURNING id",
                Long.class, authorId);
        seedTranslations("qna_translations", "qna_id", id, title, content);
        return id;
    }

    private void answer(long qnaId, long authorId, String content) {
        Long id = jdbc.queryForObject(
                "INSERT INTO answers(qna_id, author_id, is_anonymous, is_adopted, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, false, false, 0, now(), now()) RETURNING id",
                Long.class, qnaId, authorId);
        seedTranslations("answer_translations", "answer_id", id, null, content);
    }

    /**
     * KO 원문 행 + (Azure 키가 있으면) 나머지 5개 언어 번역 행을 함께 insert한다.
     * 키가 없거나 호출 실패 시 KO 원문 1행만 저장(앱은 원문 폴백으로 정상 동작).
     * title이 null이면 content만 있는 테이블(comment/answer 번역).
     */
    private void seedTranslations(String table, String fkCol, long id, String title, String content) {
        boolean hasTitle = title != null;
        if (hasTitle) {
            jdbc.update("INSERT INTO " + table + "(" + fkCol + ", language, title, content) VALUES (?, 'KO', ?, ?)",
                    id, title, content);
        } else {
            jdbc.update("INSERT INTO " + table + "(" + fkCol + ", language, content) VALUES (?, 'KO', ?)",
                    id, content);
        }

        try {
            List<String> texts = hasTitle ? List.of(title, content) : List.of(content);
            List<AzureTranslateClient.TranslatedText> results = azureClient.translate(texts, OTHER_CODES, "ko");
            if (results.isEmpty()) return; // 키 없음/응답 없음 → KO만 유지

            for (int i = 0; i < OTHER_LANGS.size(); i++) {
                String lang = OTHER_LANGS.get(i);
                if (hasTitle) {
                    String tTitle = results.get(0).translations().get(i);
                    String tContent = results.get(1).translations().get(i);
                    if (tTitle != null && tContent != null) {
                        jdbc.update("INSERT INTO " + table + "(" + fkCol + ", language, title, content) VALUES (?, ?, ?, ?)",
                                id, lang, tTitle, tContent);
                    }
                } else {
                    String tContent = results.get(0).translations().get(i);
                    if (tContent != null) {
                        jdbc.update("INSERT INTO " + table + "(" + fkCol + ", language, content) VALUES (?, ?, ?)",
                                id, lang, tContent);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[LocalSeed] 번역 생략(키 없음/실패) {}#{}: {}", table, id, e.getMessage());
        }
    }

    private void chat(long senderId, long receiverId, String content, boolean read) {
        jdbc.update(
                "INSERT INTO chat_messages(sender_id, receiver_id, context_partner_id, content, is_system, is_read, sent_at) " +
                        "VALUES (?, ?, NULL, ?, false, ?, now())",
                senderId, receiverId, content, read);
    }

    private void match(long mentorId, long menteeId, String semester) {
        jdbc.update(
                "INSERT INTO mentor_mentee_matches(mentor_id, mentee_id, semester, status, matched_at) " +
                        "VALUES (?, ?, ?, 'ACTIVE', now())",
                mentorId, menteeId, semester);
    }

}
