package com.khu.globalhub.domain.board.service;

import com.khu.globalhub.domain.board.dto.CreatePostRequest;
import com.khu.globalhub.domain.board.dto.PostDetailResponse;
import com.khu.globalhub.domain.board.dto.PostSummaryResponse;
import com.khu.globalhub.domain.board.entity.Post;
import com.khu.globalhub.domain.board.entity.PostLike;
import com.khu.globalhub.domain.board.entity.PostTranslation;
import com.khu.globalhub.domain.board.repository.PostLikeRepository;
import com.khu.globalhub.domain.board.repository.PostRepository;
import com.khu.globalhub.domain.board.repository.PostTranslationRepository;
import com.khu.globalhub.domain.anonymous.service.AnonymousAliasService;
import com.khu.globalhub.domain.comment.entity.Comment;
import com.khu.globalhub.domain.comment.repository.CommentLikeRepository;
import com.khu.globalhub.domain.comment.repository.CommentRepository;
import com.khu.globalhub.profile.domain.Profile;
import com.khu.globalhub.profile.infrastructure.ProfileRepository;
import com.khu.globalhub.shared.enums.AliasContextType;
import com.khu.globalhub.shared.enums.BoardType;
import com.khu.globalhub.shared.enums.CommentTargetType;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.S3Service;
import com.khu.globalhub.shared.infra.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostTranslationRepository postTranslationRepository;
    private final PostLikeRepository postLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ProfileRepository profileRepository;
    private final TranslationService translationService;
    private final S3Service s3Service;
    private final AnonymousAliasService anonymousAliasService;

    /**
     * 게시글 작성.
     * 1) Post 저장
     * 2) 원문 PostTranslation 동기 저장
     * 3) 나머지 5개 언어 번역 @Async 실행
     * 4) 이미지가 있으면 S3 업로드 @Async 실행
     */
    @Transactional
    public Long createPost(Long memberId, CreatePostRequest req, List<MultipartFile> images) {
        Post post = Post.builder()
                .authorId(memberId)
                .boardType(req.boardType())
                .isAnonymous(req.isAnonymous())
                .build();
        postRepository.save(post);

        // 원문 번역 행 동기 저장
        postTranslationRepository.save(PostTranslation.builder()
                .post(post)
                .language(req.language())
                .title(req.title())
                .content(req.content())
                .build());

        // 익명 게시 시 작성자에게 익명1 할당
        if (req.isAnonymous()) {
            anonymousAliasService.assign(AliasContextType.POST, post.getId(), memberId);
        }

        // 나머지 5개 언어 비동기 번역
        translationService.translatePost(post, req.title(), req.content(), req.language());

        // 이미지 비동기 S3 업로드
        if (images != null && !images.isEmpty()) {
            List<S3Service.ImageData> imageDataList = new ArrayList<>();
            for (MultipartFile file : images) {
                try {
                    imageDataList.add(new S3Service.ImageData(
                            file.getBytes(), file.getContentType()));
                } catch (IOException e) {
                    // 파일 읽기 실패 시 해당 이미지만 스킵
                }
            }
            if (!imageDataList.isEmpty()) {
                s3Service.uploadPostImages(post, imageDataList);
            }
        }

        return post.getId();
    }

    /** 게시판별 목록 조회. 요청자의 언어 설정에 맞는 번역본 반환. */
    public Page<PostSummaryResponse> getPostList(BoardType boardType, Language language, Pageable pageable) {
        return postRepository.findByBoardTypeOrderByCreatedAtDesc(boardType, pageable)
                .map(post -> {
                    PostTranslation translation = postTranslationRepository
                            .findByPostIdAndLanguage(post.getId(), language)
                            .orElseGet(() -> postTranslationRepository
                                    .findByPostIdAndLanguage(post.getId(), Language.EN)
                                    .orElseGet(() -> post.getTranslations().get(0)));
                    String authorName = resolveAuthorName(post.getIsAnonymous(),
                            AliasContextType.POST, post.getId(), post.getAuthorId());
                    return PostSummaryResponse.of(post, translation, authorName);
                });
    }

    /** 인기 게시물 목록 조회 (좋아요 10개 이상, 좋아요 많은 순). */
    public Page<PostSummaryResponse> getPopularPosts(Language language, Pageable pageable) {
        return postRepository.findByLikeCountGreaterThanEqualOrderByLikeCountDesc(10, pageable)
                .map(post -> {
                    PostTranslation translation = postTranslationRepository
                            .findByPostIdAndLanguage(post.getId(), language)
                            .orElseGet(() -> postTranslationRepository
                                    .findByPostIdAndLanguage(post.getId(), Language.EN)
                                    .orElseGet(() -> post.getTranslations().get(0)));
                    String authorName = resolveAuthorName(post.getIsAnonymous(),
                            AliasContextType.POST, post.getId(), post.getAuthorId());
                    return PostSummaryResponse.of(post, translation, authorName);
                });
    }

    /** 게시글 상세 조회. */
    public PostDetailResponse getPost(Long postId, Long memberId, Language language) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        PostTranslation translation = postTranslationRepository
                .findByPostIdAndLanguage(postId, language)
                .orElseGet(() -> postTranslationRepository
                        .findByPostIdAndLanguage(postId, Language.EN)
                        .orElseGet(() -> post.getTranslations().get(0)));

        // 원문 언어: ID가 가장 낮은(가장 먼저 저장된) 번역 행의 language
        Language originalLanguage = postTranslationRepository
                .findFirstByPostIdOrderByIdAsc(postId)
                .map(PostTranslation::getLanguage)
                .orElse(Language.KO);

        String authorName = resolveAuthorName(post.getIsAnonymous(),
                AliasContextType.POST, postId, post.getAuthorId());
        boolean isLiked = postLikeRepository.existsByMemberIdAndPostId(memberId, postId);
        boolean isOwner = post.getAuthorId().equals(memberId);

        return PostDetailResponse.of(post, translation, authorName, isLiked, isOwner, originalLanguage);
    }

    /** 게시글 삭제 (작성자 본인만 가능). 댓글·좋아요 모두 cascade 삭제. */
    @Transactional
    public void deletePost(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        if (!post.getAuthorId().equals(memberId)) {
            throw new CustomException(ErrorCode.POST_UNAUTHORIZED);
        }

        // 1) 이 게시글의 모든 댓글 ID 수집 (대댓글 포함)
        List<Comment> topLevel = commentRepository
                .findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtAsc(CommentTargetType.POST, postId);
        List<Long> allCommentIds = topLevel.stream()
                .flatMap(c -> {
                    List<Long> ids = new ArrayList<>();
                    ids.add(c.getId());
                    c.getChildren().forEach(child -> ids.add(child.getId()));
                    return ids.stream();
                })
                .collect(Collectors.toList());

        // 2) 댓글 좋아요 삭제 → 댓글 삭제
        if (!allCommentIds.isEmpty()) {
            commentLikeRepository.deleteByCommentIdIn(allCommentIds);
        }
        commentRepository.deleteAll(topLevel);

        // 3) 게시글 좋아요 삭제
        postLikeRepository.deleteByPostId(postId);

        // 4) 게시글 삭제 (PostTranslation, PostImage cascade)
        postRepository.delete(post);
    }

    /** 좋아요 토글. 이미 좋아요면 취소, 아니면 추가. */
    @Transactional
    public boolean toggleLike(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (postLikeRepository.existsByMemberIdAndPostId(memberId, postId)) {
            postLikeRepository.deleteByMemberIdAndPostId(memberId, postId);
            post.decreaseLikeCount();
            return false; // 취소됨
        } else {
            postLikeRepository.save(PostLike.builder().memberId(memberId).post(post).build());
            post.increaseLikeCount();
            return true; // 추가됨
        }
    }

    private String resolveAuthorName(boolean isAnonymous, AliasContextType ctx, Long contextId, Long authorId) {
        if (isAnonymous) {
            return anonymousAliasService.lookup(ctx, contextId, authorId);
        }
        return profileRepository.findByMemberId(authorId).map(Profile::getName).orElse("Unknown");
    }
}
