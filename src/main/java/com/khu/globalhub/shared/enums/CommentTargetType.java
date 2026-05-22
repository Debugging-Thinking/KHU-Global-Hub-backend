package com.khu.globalhub.shared.enums;

/**
 * 댓글이 달리는 대상의 종류.
 * Comment 테이블 하나로 게시글/QnA질문/QnA답변의 댓글을 통합 관리한다.
 * target_type + target_id 조합으로 어느 게시물의 댓글인지 식별한다.
 *
 * POST   : 게시판 게시글의 댓글
 * QNA    : Q&A 질문의 댓글
 * ANSWER : Q&A 답변의 댓글
 */
public enum CommentTargetType {
    POST,
    QNA,
    ANSWER
}
