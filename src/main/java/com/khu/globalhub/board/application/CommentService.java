package com.khu.globalhub.board.application;

import com.khu.globalhub.shared.anonymous.service.AnonymousAliasService;
import com.khu.globalhub.board.presentation.dto.CommentResponse;
import com.khu.globalhub.board.presentation.dto.CreateCommentRequest;
import com.khu.globalhub.board.domain.Comment;
import com.khu.globalhub.board.domain.CommentLike;
import com.khu.globalhub.board.domain.CommentTranslation;
import com.khu.globalhub.board.infrastructure.CommentLikeRepository;
import com.khu.globalhub.board.infrastructure.CommentRepository;
import com.khu.globalhub.board.infrastructure.CommentTranslationRepository;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import com.khu.globalhub.board.domain.Post;
import com.khu.globalhub.board.infrastructure.PostRepository;
import com.khu.globalhub.shared.enums.AliasContextType;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * 게시글 댓글 서비스 (board BC 전용 — D3/D4로 QnA 댓글 폐기).
 * 익명 번호는 게시글(POST) 컨텍스트를 공유한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentTranslationRepository commentTranslationRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final ProfileQueryPort profileQueryPort;
    private final TranslationService translationService;
    private final AnonymousAliasService anonymousAliasService;

    /**
     * 게시글 댓글 작성.
     * 1) 게시글 존재 검증
     * 2) Comment 저장 (원문 번역 동기, 나머지 5개 언어 @Async)
     * 3) 게시글 commentCount 증가 (대댓글 포함)
     * 4) 익명 댓글이면 POST 컨텍스트 alias 할당
     */
    @Transactional
    public Long createComment(Long postId, Long memberId, CreateCommentRequest req) {
        // 내용/첨부 중 하나는 있어야 함 (이미지만으로도 작성 허용)
        boolean hasContent = req.content() != null && !req.content().isBlank();
        boolean hasImage = req.imageUrl() != null && !req.imageUrl().isBlank();
        if (!hasContent && !hasImage) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String content = req.content() == null ? "" : req.content();

        postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment parent = null;
        if (req.parentId() != null) {
            parent = commentRepository.findById(req.parentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .targetId(postId)
                .parent(parent)
                .authorId(memberId)
                .isAnonymous(req.isAnonymous())
                .imageUrl(req.imageUrl())
                .build();
        commentRepository.save(comment);

        // 원문 번역 행 동기 저장 (내용 없으면 빈 문자열)
        commentTranslationRepository.save(CommentTranslation.builder()
                .comment(comment)
                .language(req.language())
                .content(content)
                .build());

        // 내용이 있을 때만 나머지 5개 언어 비동기 번역 (이미지만 있으면 번역 불필요)
        if (hasContent) {
            translationService.translateComment(comment, content, req.language());
        }

        // 게시글 commentCount 증가 (대댓글도 카운트)
        postRepository.findById(postId).ifPresent(Post::incrementCommentCount);

        // 익명 댓글: 게시글 컨텍스트에서 alias 할당
        if (req.isAnonymous()) {
            anonymousAliasService.assign(AliasContextType.POST, postId, memberId);
        }

        return comment.getId();
    }

    /**
     * 게시글 댓글 목록 조회.
     * original=false: 요청자 언어 번역본. original=true: 원문(소스) 행 (6개 외 언어 사용자용).
     */
    public List<CommentResponse> getComments(Long postId, Long memberId, Language language, boolean original) {
        List<Comment> topLevel = commentRepository
                .findByTargetIdAndParentIsNullOrderByCreatedAtAsc(postId);

        return topLevel.stream()
                .map(comment -> buildCommentResponse(comment, memberId, language, original))
                .toList();
    }

    /** 댓글 삭제 (작성자 본인만). 대댓글·좋아요 cascade 삭제, 게시글 commentCount 감소. */
    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthorId().equals(memberId)) {
            throw new CustomException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        int count = 1 + comment.getChildren().size();
        postRepository.findById(comment.getTargetId())
                .ifPresent(post -> {
                    for (int i = 0; i < count; i++) post.decrementCommentCount();
                });

        // 이 댓글 + 대댓글 좋아요 먼저 삭제
        List<Long> allIds = new ArrayList<>();
        allIds.add(commentId);
        comment.getChildren().forEach(child -> allIds.add(child.getId()));
        commentLikeRepository.deleteByCommentIdIn(allIds);

        commentRepository.delete(comment);
    }

    /** 댓글 좋아요 토글. */
    @Transactional
    public boolean toggleLike(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        if (commentLikeRepository.existsByMemberIdAndCommentId(memberId, commentId)) {
            commentLikeRepository.deleteByMemberIdAndCommentId(memberId, commentId);
            comment.decreaseLikeCount();
            return false;
        } else {
            commentLikeRepository.save(CommentLike.builder().memberId(memberId).comment(comment).build());
            comment.increaseLikeCount();
            return true;
        }
    }

    private CommentResponse buildCommentResponse(Comment comment, Long memberId, Language language, boolean original) {
        CommentTranslation translation = original
                ? commentTranslationRepository.findFirstByCommentIdOrderByIdAsc(comment.getId())
                        .orElseGet(() -> comment.getTranslations().get(0))
                : commentTranslationRepository
                        .findByCommentIdAndLanguage(comment.getId(), language)
                        .orElseGet(() -> commentTranslationRepository
                                .findByCommentIdAndLanguage(comment.getId(), Language.EN)
                                .orElseGet(() -> comment.getTranslations().get(0)));
        // 항목별 원문(소스) 행 — 댓글마다 원문 언어가 다를 수 있어 함께 내려준다.
        CommentTranslation source = commentTranslationRepository.findFirstByCommentIdOrderByIdAsc(comment.getId())
                .orElse(translation);

        // 익명 번호는 게시글(POST) 컨텍스트 공유 — targetId = postId
        String authorName = comment.getIsAnonymous()
                ? anonymousAliasService.lookup(AliasContextType.POST, comment.getTargetId(), comment.getAuthorId())
                : profileQueryPort.findName(comment.getAuthorId()).orElse("Unknown");
        boolean isLiked = commentLikeRepository.existsByMemberIdAndCommentId(memberId, comment.getId());
        boolean isOwner = comment.getAuthorId().equals(memberId);

        // 대댓글 재귀 변환
        List<CommentResponse> children = comment.getChildren().stream()
                .map(child -> buildCommentResponse(child, memberId, language, original))
                .toList();

        return CommentResponse.of(comment, translation, source, authorName, isLiked, isOwner, children);
    }
}
