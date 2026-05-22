package com.khu.globalhub.domain.comment.service;

import com.khu.globalhub.domain.anonymous.service.AnonymousAliasService;
import com.khu.globalhub.domain.comment.dto.CommentResponse;
import com.khu.globalhub.domain.comment.dto.CreateCommentRequest;
import com.khu.globalhub.domain.comment.entity.Comment;
import com.khu.globalhub.domain.comment.entity.CommentLike;
import com.khu.globalhub.domain.comment.entity.CommentTranslation;
import com.khu.globalhub.domain.comment.repository.CommentLikeRepository;
import com.khu.globalhub.domain.comment.repository.CommentRepository;
import com.khu.globalhub.domain.comment.repository.CommentTranslationRepository;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import com.khu.globalhub.domain.board.entity.Post;
import com.khu.globalhub.domain.board.repository.PostRepository;
import com.khu.globalhub.domain.qna.entity.Answer;
import com.khu.globalhub.domain.qna.repository.QnARepository;
import com.khu.globalhub.domain.qna.repository.AnswerRepository;
import com.khu.globalhub.shared.enums.AliasContextType;
import com.khu.globalhub.shared.enums.CommentTargetType;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final CommentRepository commentRepository;
    private final CommentTranslationRepository commentTranslationRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final PostRepository postRepository;
    private final QnARepository qnaRepository;
    private final AnswerRepository answerRepository;
    private final ProfileQueryPort profileQueryPort;
    private final TranslationService translationService;
    private final AnonymousAliasService anonymousAliasService;

    /**
     * 댓글 작성 (게시글/Q&A/답변 공통).
     * 1) 대상 존재 검증
     * 2) Comment 저장
     * 3) 원문 CommentTranslation 동기 저장
     * 4) 나머지 5개 언어 번역 @Async
     * 5) POST 타입인 경우 commentCount 증가
     */
    @Transactional
    public Long createComment(CommentTargetType targetType, Long targetId, Long memberId, CreateCommentRequest req) {
        validateTarget(targetType, targetId);

        Comment parent = null;
        if (req.parentId() != null) {
            parent = commentRepository.findById(req.parentId())
                    .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        }

        Comment comment = Comment.builder()
                .targetType(targetType)
                .targetId(targetId)
                .parent(parent)
                .authorId(memberId)
                .isAnonymous(req.isAnonymous())
                .build();
        commentRepository.save(comment);

        // 원문 번역 행 동기 저장
        commentTranslationRepository.save(CommentTranslation.builder()
                .comment(comment)
                .language(req.language())
                .content(req.content())
                .build());

        // 나머지 5개 언어 비동기 번역
        translationService.translateComment(comment, req.content(), req.language());

        // 게시글 commentCount 증가 (대댓글도 카운트)
        if (targetType == CommentTargetType.POST) {
            postRepository.findById(targetId).ifPresent(Post::incrementCommentCount);
        }

        // 익명 댓글: alias 할당 (POST → POST 컨텍스트, QNA/ANSWER → QNA 컨텍스트)
        if (req.isAnonymous()) {
            AliasContextType ctx = resolveAliasContext(targetType);
            Long ctxId = resolveAliasContextId(targetType, targetId);
            anonymousAliasService.assign(ctx, ctxId, memberId);
        }

        return comment.getId();
    }

    /** 댓글 목록 조회 (게시글/Q&A/답변 공통, 요청자 언어 적용). */
    public List<CommentResponse> getComments(CommentTargetType targetType, Long targetId,
                                             Long memberId, Language language) {
        AliasContextType aliasCtx = resolveAliasContext(targetType);
        Long aliasCtxId = resolveAliasContextId(targetType, targetId);

        List<Comment> topLevel = commentRepository
                .findByTargetTypeAndTargetIdAndParentIsNullOrderByCreatedAtAsc(targetType, targetId);

        return topLevel.stream()
                .map(comment -> buildCommentResponse(comment, memberId, language, aliasCtx, aliasCtxId))
                .toList();
    }

    /** targetType → alias 컨텍스트 타입 변환 (ANSWER도 QNA 컨텍스트 사용). */
    private AliasContextType resolveAliasContext(CommentTargetType targetType) {
        return targetType == CommentTargetType.POST ? AliasContextType.POST : AliasContextType.QNA;
    }

    /** targetType/targetId → alias 컨텍스트 ID 변환. ANSWER이면 qnaId 반환. */
    private Long resolveAliasContextId(CommentTargetType targetType, Long targetId) {
        if (targetType == CommentTargetType.ANSWER) {
            Answer answer = answerRepository.findById(targetId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));
            return answer.getQna().getId();
        }
        return targetId;
    }

    private void validateTarget(CommentTargetType targetType, Long targetId) {
        switch (targetType) {
            case POST -> postRepository.findById(targetId)
                    .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
            case QNA -> qnaRepository.findById(targetId)
                    .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
            case ANSWER -> answerRepository.findById(targetId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));
        }
    }

    /** 댓글 삭제 (작성자 본인만). 대댓글·좋아요 cascade 삭제. */
    @Transactional
    public void deleteComment(Long commentId, Long memberId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
        if (!comment.getAuthorId().equals(memberId)) {
            throw new CustomException(ErrorCode.COMMENT_UNAUTHORIZED);
        }

        // 게시글 댓글인 경우에만 commentCount 감소
        if (comment.getTargetType() == CommentTargetType.POST) {
            int count = 1 + comment.getChildren().size();
            postRepository.findById(comment.getTargetId())
                    .ifPresent(post -> {
                        for (int i = 0; i < count; i++) post.decrementCommentCount();
                    });
        }

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

    private CommentResponse buildCommentResponse(Comment comment, Long memberId, Language language,
                                                  AliasContextType aliasCtx, Long aliasCtxId) {
        CommentTranslation translation = commentTranslationRepository
                .findByCommentIdAndLanguage(comment.getId(), language)
                .orElseGet(() -> commentTranslationRepository
                        .findByCommentIdAndLanguage(comment.getId(), Language.EN)
                        .orElseGet(() -> comment.getTranslations().get(0)));

        String authorName = comment.getIsAnonymous()
                ? anonymousAliasService.lookup(aliasCtx, aliasCtxId, comment.getAuthorId())
                : profileQueryPort.findName(comment.getAuthorId()).orElse("Unknown");
        boolean isLiked = commentLikeRepository.existsByMemberIdAndCommentId(memberId, comment.getId());
        boolean isOwner = comment.getAuthorId().equals(memberId);

        // 대댓글 재귀 변환
        List<CommentResponse> children = comment.getChildren().stream()
                .map(child -> buildCommentResponse(child, memberId, language, aliasCtx, aliasCtxId))
                .toList();

        return CommentResponse.of(comment, translation, authorName, isLiked, isOwner, children);
    }
}
