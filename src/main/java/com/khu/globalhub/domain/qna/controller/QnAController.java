package com.khu.globalhub.domain.qna.controller;

import com.khu.globalhub.domain.qna.dto.*;
import com.khu.globalhub.domain.qna.service.QnAService;
import com.khu.globalhub.global.common.ApiResponse;
import com.khu.globalhub.global.enums.Language;
import com.khu.globalhub.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qnas")
@RequiredArgsConstructor
public class QnAController {

    private final QnAService qnaService;

    // ───────── QnA ─────────

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createQnA(
            @Valid @RequestBody CreateQnARequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long qnaId = qnaService.createQnA(memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("질문이 등록되었습니다.", qnaId));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<QnASummaryResponse>>> getQnAList(
            @RequestParam(defaultValue = "KO") Language language,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<QnASummaryResponse> result = qnaService.getQnAList(language, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{qnaId}")
    public ResponseEntity<ApiResponse<QnADetailResponse>> getQnA(
            @PathVariable Long qnaId,
            @RequestParam(defaultValue = "KO") Language language
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        QnADetailResponse result = qnaService.getQnA(qnaId, memberId, language);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @DeleteMapping("/{qnaId}")
    public ResponseEntity<ApiResponse<Void>> deleteQnA(@PathVariable Long qnaId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        qnaService.deleteQnA(qnaId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("Q&A가 삭제되었습니다."));
    }

    @PostMapping("/{qnaId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleQnALike(@PathVariable Long qnaId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        boolean liked = qnaService.toggleQnALike(qnaId, memberId);
        String msg = liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, liked));
    }

    // ───────── Answer ─────────

    @PostMapping("/{qnaId}/answers")
    public ResponseEntity<ApiResponse<Long>> createAnswer(
            @PathVariable Long qnaId,
            @Valid @RequestBody CreateAnswerRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long answerId = qnaService.createAnswer(qnaId, memberId, request);
        return ResponseEntity.ok(ApiResponse.ok("답변이 등록되었습니다.", answerId));
    }

    @DeleteMapping("/{qnaId}/answers/{answerId}")
    public ResponseEntity<ApiResponse<Void>> deleteAnswer(
            @PathVariable Long qnaId,
            @PathVariable Long answerId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        qnaService.deleteAnswer(answerId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("답변이 삭제되었습니다."));
    }

    /**
     * 답변 채택. 질문 작성자만 가능, 이미 채택 완료된 경우 409.
     */
    @PostMapping("/{qnaId}/answers/{answerId}/adopt")
    public ResponseEntity<ApiResponse<Void>> adoptAnswer(
            @PathVariable Long qnaId,
            @PathVariable Long answerId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        qnaService.adoptAnswer(qnaId, answerId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("답변이 채택되었습니다."));
    }

    @PostMapping("/{qnaId}/answers/{answerId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleAnswerLike(
            @PathVariable Long qnaId,
            @PathVariable Long answerId
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        boolean liked = qnaService.toggleAnswerLike(answerId, memberId);
        String msg = liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, liked));
    }
}
