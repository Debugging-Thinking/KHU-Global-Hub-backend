package com.khu.globalhub.domain.board.controller;

import com.khu.globalhub.domain.board.dto.CreatePostRequest;
import com.khu.globalhub.domain.board.dto.PostDetailResponse;
import com.khu.globalhub.domain.board.dto.PostSummaryResponse;
import com.khu.globalhub.domain.board.service.PostService;
import com.khu.globalhub.global.common.ApiResponse;
import com.khu.globalhub.global.enums.BoardType;
import com.khu.globalhub.global.enums.Language;
import com.khu.globalhub.global.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /**
     * 게시글 작성.
     * 이미지 첨부가 있을 수 있어 multipart/form-data 사용.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Long>> createPost(
            @Valid @RequestPart("request") CreatePostRequest request,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        Long postId = postService.createPost(memberId, request, images);
        return ResponseEntity.ok(ApiResponse.ok("게시글이 작성되었습니다.", postId));
    }

    /**
     * 게시판별 목록 조회.
     * boardType: FRESHMAN / FREE / GRADUATE
     * language: 조회자의 언어 (번역본 선택용)
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getPostList(
            @RequestParam BoardType boardType,
            @RequestParam(defaultValue = "KO") Language language,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<PostSummaryResponse> result = postService.getPostList(boardType, language, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 인기 게시물 목록 조회.
     * 좋아요 10개 이상인 게시글을 좋아요 많은 순으로 반환.
     * NOTE: /popular는 /{postId} 보다 먼저 선언해야 Spring이 정적 경로를 우선 매핑한다.
     */
    @GetMapping("/popular")
    public ResponseEntity<ApiResponse<Page<PostSummaryResponse>>> getPopularPosts(
            @RequestParam(defaultValue = "KO") Language language,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<PostSummaryResponse> result = postService.getPopularPosts(language, pageable);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 게시글 상세 조회. */
    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPost(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "KO") Language language
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        PostDetailResponse result = postService.getPost(postId, memberId, language);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 게시글 삭제. */
    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long postId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        postService.deletePost(postId, memberId);
        return ResponseEntity.ok(ApiResponse.ok("게시글이 삭제되었습니다."));
    }

    /** 좋아요 토글. */
    @PostMapping("/{postId}/like")
    public ResponseEntity<ApiResponse<Boolean>> toggleLike(@PathVariable Long postId) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        boolean liked = postService.toggleLike(postId, memberId);
        String msg = liked ? "좋아요를 눌렀습니다." : "좋아요를 취소했습니다.";
        return ResponseEntity.ok(ApiResponse.ok(msg, liked));
    }
}
