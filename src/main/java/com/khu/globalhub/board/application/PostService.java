package com.khu.globalhub.board.application;

import com.khu.globalhub.board.presentation.dto.CreatePostRequest;
import com.khu.globalhub.board.presentation.dto.PostDetailResponse;
import com.khu.globalhub.board.presentation.dto.PostSummaryResponse;
import com.khu.globalhub.board.domain.Post;
import com.khu.globalhub.board.domain.PostLike;
import com.khu.globalhub.board.domain.PostTranslation;
import com.khu.globalhub.board.infrastructure.PostLikeRepository;
import com.khu.globalhub.board.infrastructure.PostRepository;
import com.khu.globalhub.board.infrastructure.PostTranslationRepository;
import com.khu.globalhub.shared.anonymous.service.AnonymousAliasService;
import com.khu.globalhub.board.domain.Comment;
import com.khu.globalhub.board.infrastructure.CommentLikeRepository;
import com.khu.globalhub.board.infrastructure.CommentRepository;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import com.khu.globalhub.shared.port.MemberQueryPort;
import com.khu.globalhub.shared.enums.AliasContextType;
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
    private final ProfileQueryPort profileQueryPort;
    private final MemberQueryPort memberQueryPort;
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
                            file.getBytes(), file.getContentType(), file.getOriginalFilename()));
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

    /**
     * 게시글 수정 (작성자 본인만). 제목/내용 갱신 + 나머지 언어 재번역. 새 이미지는 추가.
     */
    @Transactional
    public void updatePost(Long postId, Long memberId, CreatePostRequest req, List<MultipartFile> images) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        if (!post.getAuthorId().equals(memberId)) {
            throw new CustomException(ErrorCode.POST_UNAUTHORIZED);
        }

        // 작성자 언어 원문 행 upsert
        postTranslationRepository.findByPostIdAndLanguage(postId, req.language())
                .ifPresentOrElse(
                        t -> { t.updateContent(req.title(), req.content()); postTranslationRepository.save(t); },
                        () -> postTranslationRepository.save(PostTranslation.builder()
                                .post(post).language(req.language())
                                .title(req.title()).content(req.content()).build()));

        // 나머지 언어 재번역(upsert)
        translationService.translatePost(post, req.title(), req.content(), req.language());

        // 새로 첨부한 이미지가 있으면 추가 업로드(기존 이미지 유지)
        if (images != null && !images.isEmpty()) {
            List<S3Service.ImageData> imageDataList = new ArrayList<>();
            for (MultipartFile file : images) {
                try {
                    imageDataList.add(new S3Service.ImageData(
                            file.getBytes(), file.getContentType(), file.getOriginalFilename()));
                } catch (IOException e) {
                    // 파일 읽기 실패 시 해당 이미지만 스킵
                }
            }
            if (!imageDataList.isEmpty()) {
                s3Service.uploadPostImages(post, imageDataList);
            }
        }
    }

    /**
     * 게시글 목록 조회 (게시판 1종).
     * original=false: 요청자 언어 번역본(폴백 요청언어→EN→첫행).
     * original=true: 원문(소스) 행 — 6개 외 언어 사용자가 "번역하기" 전 원문을 볼 때 사용.
     */
    public Page<PostSummaryResponse> getPostList(Language language, boolean original, Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(post -> {
                    PostTranslation translation = resolvePostTranslation(post, language, original);
                    String authorName = resolveAuthorName(post.getIsAnonymous(),
                            AliasContextType.POST, post.getId(), post.getAuthorId());
                    return PostSummaryResponse.of(post, translation, authorName);
                });
    }

    /** 인기 게시물 목록 조회 (좋아요 10개 이상, 좋아요 많은 순). */
    public Page<PostSummaryResponse> getPopularPosts(Language language, boolean original, Pageable pageable) {
        return postRepository.findByLikeCountGreaterThanEqualOrderByLikeCountDesc(10, pageable)
                .map(post -> {
                    PostTranslation translation = resolvePostTranslation(post, language, original);
                    String authorName = resolveAuthorName(post.getIsAnonymous(),
                            AliasContextType.POST, post.getId(), post.getAuthorId());
                    return PostSummaryResponse.of(post, translation, authorName);
                });
    }

    /** 게시글 상세 조회. */
    public PostDetailResponse getPost(Long postId, Long memberId, Language language, boolean original) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        PostTranslation translation = resolvePostTranslation(post, language, original);
        // 항목별 원문(소스) 행 — 제목/본문/언어를 함께 내려 클라가 항목별 원문/번역 토글.
        PostTranslation source = postTranslationRepository.findFirstByPostIdOrderByIdAsc(postId)
                .orElse(translation);

        String authorName = resolveAuthorName(post.getIsAnonymous(),
                AliasContextType.POST, postId, post.getAuthorId());
        boolean isLiked = postLikeRepository.existsByMemberIdAndPostId(memberId, postId);
        boolean isOwner = post.getAuthorId().equals(memberId);

        return PostDetailResponse.of(post, translation, source, authorName, isLiked, isOwner);
    }

    /** 게시글 삭제 (작성자 본인만 가능). 댓글·좋아요 모두 cascade 삭제. */
    @Transactional
    public void deletePost(Long postId, Long memberId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        if (!post.getAuthorId().equals(memberId) && !memberQueryPort.isAdmin(memberId)) {
            throw new CustomException(ErrorCode.POST_UNAUTHORIZED);
        }

        // 1) 이 게시글의 모든 댓글 ID 수집 (대댓글 포함)
        List<Comment> topLevel = commentRepository
                .findByTargetIdAndParentIsNullOrderByCreatedAtAsc(postId);
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

    /**
     * 특정 멤버가 쓴 게시글 목록 (board 소유 데이터).
     * - 본인(myId == targetMemberId): 익명 게시글 포함
     * - 타인: 익명 게시글 제외
     * 작성자 이름은 ProfileQueryPort로 조회.
     */
    public Page<PostSummaryResponse> getMemberPosts(Long myId, Long targetMemberId,
                                                    Language language, boolean original, Pageable pageable) {
        boolean isOwner = myId.equals(targetMemberId);

        var posts = isOwner
                ? postRepository.findByAuthorIdOrderByCreatedAtDesc(targetMemberId, pageable)
                : postRepository.findByAuthorIdAndIsAnonymousFalseOrderByCreatedAtDesc(targetMemberId, pageable);

        return posts.map(post -> {
            PostTranslation translation = resolvePostTranslation(post, language, original);
            String authorName = profileQueryPort.findName(post.getAuthorId()).orElse("Unknown");
            return PostSummaryResponse.of(post, translation, authorName);
        });
    }

    /**
     * 게시글 번역본 선택.
     * original=true → 원문(소스) 행(가장 먼저 저장된 행, 자동감지로 라벨 보정됨).
     * original=false → 요청언어 → EN → 첫 행 폴백.
     */
    private PostTranslation resolvePostTranslation(Post post, Language language, boolean original) {
        if (original) {
            return postTranslationRepository.findFirstByPostIdOrderByIdAsc(post.getId())
                    .orElseGet(() -> post.getTranslations().get(0));
        }
        return postTranslationRepository
                .findByPostIdAndLanguage(post.getId(), language)
                .orElseGet(() -> postTranslationRepository
                        .findByPostIdAndLanguage(post.getId(), Language.EN)
                        .orElseGet(() -> post.getTranslations().get(0)));
    }

    private String resolveAuthorName(boolean isAnonymous, AliasContextType ctx, Long contextId, Long authorId) {
        if (isAnonymous) {
            return anonymousAliasService.lookup(ctx, contextId, authorId);
        }
        return profileQueryPort.findName(authorId).orElse("Unknown");
    }
}
