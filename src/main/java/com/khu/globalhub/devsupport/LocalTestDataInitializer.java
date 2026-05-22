package com.khu.globalhub.devsupport;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 로컬 개발용 테스트 데이터 시드 (local 프로필 전용).
 *
 * <p>로컬은 SMTP가 더미라 회원가입(이메일 인증)이 안 되므로, 팀원이 매번 계정을 만들 필요 없이
 * 표준 테스트 계정과 샘플 콘텐츠를 자동 생성한다. 모든 계정 비밀번호는 {@code password123}.
 *
 * <ul>
 *   <li>{@code @Profile("local")} — 운영(prod)에서는 절대 실행되지 않는다.</li>
 *   <li>멱등 — demo 계정이 이미 있으면 건너뛴다(중복 시드 방지).</li>
 *   <li>JdbcTemplate만 사용 — 특정 BC 패키지에 의존하지 않는 순수 개발 보조 도구.</li>
 * </ul>
 *
 * 계정: demo / alice / bob / carol @khu.ac.kr
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class LocalTestDataInitializer implements ApplicationRunner {

    private static final String SENTINEL_EMAIL = "demo@khu.ac.kr";

    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        Integer count = jdbc.queryForObject(
                "SELECT count(*) FROM members WHERE email = ?", Integer.class, SENTINEL_EMAIL);
        if (count != null && count > 0) {
            log.info("[LocalSeed] 테스트 데이터가 이미 있어 시드를 건너뜁니다.");
            return;
        }

        log.info("[LocalSeed] 로컬 테스트 데이터 시드 시작…");
        String pw = passwordEncoder.encode("password123");

        long demo = member("demo@khu.ac.kr", pw);   profile(demo, "데모", "컴퓨터공학과", "대한민국", 2020, "MENTEE");
        long alice = member("alice@khu.ac.kr", pw);  profile(alice, "앨리스", "경영학과", "미국", 2024, "MENTEE");
        long bob = member("bob@khu.ac.kr", pw);      profile(bob, "밥", "전자공학과", "캐나다", 2021, "MENTOR");
        long carol = member("carol@khu.ac.kr", pw);  profile(carol, "캐롤", "국제학과", "베트남", 2023, "MENTEE");

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

        log.info("[LocalSeed] 완료 ✅  로그인 계정: demo / alice / bob / carol @khu.ac.kr  (비밀번호: password123)");
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
        jdbc.update(
                "INSERT INTO post_translations(post_id, language, title, content) VALUES (?, 'KO', ?, ?)",
                id, title, content);
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
        jdbc.update(
                "INSERT INTO comment_translations(comment_id, language, content) VALUES (?, 'KO', ?)",
                id, content);
    }

    private long qna(long authorId, String title, String content) {
        Long id = jdbc.queryForObject(
                "INSERT INTO qnas(author_id, is_anonymous, is_adopted, like_count, created_at, updated_at) " +
                        "VALUES (?, false, false, 0, now(), now()) RETURNING id",
                Long.class, authorId);
        jdbc.update(
                "INSERT INTO qna_translations(qna_id, language, title, content) VALUES (?, 'KO', ?, ?)",
                id, title, content);
        return id;
    }

    private void answer(long qnaId, long authorId, String content) {
        Long id = jdbc.queryForObject(
                "INSERT INTO answers(qna_id, author_id, is_anonymous, is_adopted, like_count, created_at, updated_at) " +
                        "VALUES (?, ?, false, false, 0, now(), now()) RETURNING id",
                Long.class, qnaId, authorId);
        jdbc.update(
                "INSERT INTO answer_translations(answer_id, language, content) VALUES (?, 'KO', ?)",
                id, content);
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
