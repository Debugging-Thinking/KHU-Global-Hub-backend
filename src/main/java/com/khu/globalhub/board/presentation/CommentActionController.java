package com.khu.globalhub.board.presentation;

import com.khu.globalhub.board.application.CommentService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 댓글 공통 액션 (삭제·좋아요).
 * commentId만 필요하므로 게시글 경로와 분리된 /api/comments/{commentId}로 제공한다.
 */
@RestController
@RequiredArgsConstructor
public class CommentActionController {

    private final CommentService commentService;

    @DeleteMapping("/api/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(@PathVariable Long commentId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        commentService.deleteComment(commentId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("댓글이 삭제되었습니다."));
    }

    @PostMapping("/api/comments/{commentId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(@PathVariable Long commentId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        boolean liked = commentService.toggleLike(commentId, memberId);
        String msg = liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, liked));
    }
}
