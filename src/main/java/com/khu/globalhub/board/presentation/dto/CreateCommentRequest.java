package com.khu.globalhub.board.presentation.dto;

import com.khu.globalhub.shared.enums.Language;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(

        /** 대댓글인 경우 부모 댓글 ID. 일반 댓글이면 null. */
        Long parentId,

        @NotNull
        Boolean isAnonymous,

        /** 작성자가 선택한 원문 언어 */
        @NotNull
        Language language,

        /** 내용 (선택 — 첨부 이미지만 있으면 비워도 됨). 서비스에서 내용/이미지 중 하나는 필수 검증. */
        String content,

        /** 첨부 이미지 URL (선택). POST /api/images로 먼저 업로드 후 URL 전달. */
        String imageUrl
) {}
