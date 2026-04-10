package com.khu.globalhub.global.enums;

/**
 * 게시판 종류.
 * 누구나 이용 가능하며, 카테고리는 어떤 글을 올릴지 유도하는 역할만 한다.
 *
 * FRESHMAN : 신입생 게시판 (정착 팁, 초기 질문 등)
 * FREE     : 자유 게시판 (주제 제한 없음)
 * GRADUATE : 졸업생 게시판 (진로, 취업 후기 등)
 */
public enum BoardType {
    FRESHMAN,
    FREE,
    GRADUATE
}
