package com.khu.globalhub.domain.comment.dto;

import com.khu.globalhub.global.enums.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateCommentRequest(

        /** 대댓글인 경우 부모 댓글 ID. 일반 댓글이면 null. */
        Long parentId,

        @NotNull
        Boolean isAnonymous,

        /** 작성자가 선택한 원문 언어 */
        @NotNull
        Language language,

        @NotBlank
        String content
) {}
