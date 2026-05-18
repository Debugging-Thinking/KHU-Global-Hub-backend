package com.khu.globalhub.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Auth ──────────────────────────────────────────
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다."),
    INVALID_EMAIL_DOMAIN(HttpStatus.BAD_REQUEST, "@khu.ac.kr 이메일만 허용됩니다."),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증이 완료되지 않았습니다."),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "유효하지 않은 인증 코드입니다."),
    VERIFICATION_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증 코드가 만료되었습니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "비밀번호가 올바르지 않습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다."),

    // ── Member ────────────────────────────────────────
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 회원입니다."),

    // ── Profile ───────────────────────────────────────
    PROFILE_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 프로필이 생성되었습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
    INVALID_MENTORING_ROLE(HttpStatus.BAD_REQUEST, "신입생은 멘티만 선택 가능합니다."),
    CANNOT_SET_NONE_ON_INIT(HttpStatus.BAD_REQUEST, "초기 프로필 생성 시 NONE은 선택할 수 없습니다."),

    // ── Post ──────────────────────────────────────────
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    POST_UNAUTHORIZED(HttpStatus.FORBIDDEN, "게시글을 수정/삭제할 권한이 없습니다."),

    // ── Comment ───────────────────────────────────────
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    COMMENT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "댓글을 수정/삭제할 권한이 없습니다."),

    // ── Q&A ───────────────────────────────────────────
    QNA_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 Q&A입니다."),
    QNA_UNAUTHORIZED(HttpStatus.FORBIDDEN, "Q&A를 수정/삭제할 권한이 없습니다."),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 답변입니다."),
    ANSWER_UNAUTHORIZED(HttpStatus.FORBIDDEN, "답변을 수정/삭제할 권한이 없습니다."),
    ALREADY_ADOPTED(HttpStatus.CONFLICT, "이미 채택된 답변이 있습니다."),
    ADOPT_UNAUTHORIZED(HttpStatus.FORBIDDEN, "채택 권한이 없습니다."),

    // ── Like ──────────────────────────────────────────
    ALREADY_LIKED(HttpStatus.CONFLICT, "이미 좋아요를 눌렀습니다."),
    LIKE_NOT_FOUND(HttpStatus.NOT_FOUND, "좋아요 기록이 없습니다."),

    // ── Mentoring ─────────────────────────────────────
    MATCH_NOT_FOUND(HttpStatus.NOT_FOUND, "현재 진행 중인 매칭이 없습니다."),

    // ── Q&A 답변 제한 ──────────────────────────────────────
    QNA_ALREADY_ADOPTED(HttpStatus.CONFLICT, "채택이 완료된 질문에는 더 이상 답변할 수 없습니다."),
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 이 질문에 답변하셨습니다."),
    SELF_ANSWER_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "본인의 질문에는 답변할 수 없습니다."),

    // ── Chat ──────────────────────────────────────────
    CANNOT_CHAT_WITH_SELF(HttpStatus.BAD_REQUEST, "자기 자신에게 메시지를 보낼 수 없습니다."),

    // ── Quiz ──────────────────────────────────────────
    QUIZ_QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 퀴즈 문항입니다."),

    // ── Common ────────────────────────────────────────
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}
