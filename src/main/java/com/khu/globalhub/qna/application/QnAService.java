package com.khu.globalhub.qna.application;

import com.khu.globalhub.shared.anonymous.service.AnonymousAliasService;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import com.khu.globalhub.shared.port.MemberQueryPort;
import com.khu.globalhub.qna.presentation.dto.*;
import com.khu.globalhub.qna.domain.*;
import com.khu.globalhub.qna.infrastructure.*;
import com.khu.globalhub.shared.enums.AliasContextType;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.infra.TranslationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QnAService {

    private final QnARepository qnaRepository;
    private final QnATranslationRepository qnaTranslationRepository;
    private final QnALikeRepository qnaLikeRepository;
    private final AnswerRepository answerRepository;
    private final AnswerTranslationRepository answerTranslationRepository;
    private final AnswerLikeRepository answerLikeRepository;
    private final ProfileQueryPort profileQueryPort;
    private final MemberQueryPort memberQueryPort;
    private final TranslationService translationService;
    private final AnonymousAliasService anonymousAliasService;

    // ───────── QnA CRUD ─────────

    @Transactional
    public Long createQnA(Long memberId, CreateQnARequest req) {
        QnA qna = QnA.builder()
                .authorId(memberId)
                .isAnonymous(req.isAnonymous())
                .imageUrl(req.imageUrl())
                .build();
        qnaRepository.save(qna);

        // 원문 번역 행 동기 저장
        qnaTranslationRepository.save(QnATranslation.builder()
                .qna(qna)
                .language(req.language())
                .title(req.title())
                .content(req.content())
                .build());

        // 나머지 5개 언어 비동기 번역
        translationService.translateQnA(qna, req.title(), req.content(), req.language());

        // 익명 질문 시 작성자에게 익명1 할당
        if (req.isAnonymous()) {
            anonymousAliasService.assign(AliasContextType.QNA, qna.getId(), memberId);
        }

        return qna.getId();
    }

    public Page<QnASummaryResponse> getQnAList(Language language, boolean original, Pageable pageable) {
        return qnaRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(qna -> {
                    QnATranslation translation = resolveQnATranslation(qna, language, original);
                    String authorName = resolveAuthorName(qna.getIsAnonymous(),
                            AliasContextType.QNA, qna.getId(), qna.getAuthorId());
                    int answerCount = answerRepository.countByQnaId(qna.getId());
                    return QnASummaryResponse.of(qna, translation, authorName, answerCount);
                });
    }

    public QnADetailResponse getQnA(Long qnaId, Long memberId, Language language, boolean original) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));

        QnATranslation translation = resolveQnATranslation(qna, language, original);
        // 질문 원문(소스) 행 — 항목별 원문/번역 토글용.
        QnATranslation source = qnaTranslationRepository.findFirstByQnaIdOrderByIdAsc(qnaId)
                .orElse(translation);
        String authorName = resolveAuthorName(qna.getIsAnonymous(),
                AliasContextType.QNA, qnaId, qna.getAuthorId());
        boolean isLiked = qnaLikeRepository.existsByMemberIdAndQnaId(memberId, qnaId);
        boolean isOwner = qna.getAuthorId().equals(memberId);

        List<AnswerResponse> answers = answerRepository.findByQnaIdOrderByCreatedAtAsc(qnaId)
                .stream()
                .map(answer -> buildAnswerResponse(answer, qnaId, memberId, language, original))
                .toList();

        return QnADetailResponse.of(qna, translation, source, authorName, isLiked, isOwner, answers);
    }

    @Transactional
    public void deleteQnA(Long qnaId, Long memberId) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
        if (!qna.getAuthorId().equals(memberId) && !memberQueryPort.isAdmin(memberId)) {
            throw new CustomException(ErrorCode.QNA_UNAUTHORIZED);
        }

        // 답변 좋아요 먼저 삭제 (answer_likes FK)
        List<Long> answerIds = qna.getAnswers().stream()
                .map(Answer::getId)
                .toList();
        if (!answerIds.isEmpty()) {
            answerLikeRepository.deleteByAnswerIdIn(answerIds);
        }

        // QnA 좋아요 삭제 (qna_likes FK)
        qnaLikeRepository.deleteByQnaId(qnaId);

        // QnA 삭제 (Answer, QnATranslation, AnswerTranslation cascade)
        qnaRepository.delete(qna);
    }

    @Transactional
    public boolean toggleQnALike(Long qnaId, Long memberId) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));

        if (qnaLikeRepository.existsByMemberIdAndQnaId(memberId, qnaId)) {
            qnaLikeRepository.deleteByMemberIdAndQnaId(memberId, qnaId);
            qna.decreaseLikeCount();
            return false;
        } else {
            qnaLikeRepository.save(QnALike.builder().memberId(memberId).qna(qna).build());
            qna.increaseLikeCount();
            return true;
        }
    }

    // ───────── Answer CRUD ─────────

    @Transactional
    public Long createAnswer(Long qnaId, Long memberId, CreateAnswerRequest req) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));

        // 채택 완료된 질문엔 답변 불가
        if (qna.getIsAdopted()) {
            throw new CustomException(ErrorCode.QNA_ALREADY_ADOPTED);
        }
        // 본인 질문엔 답변 불가
        if (qna.getAuthorId().equals(memberId)) {
            throw new CustomException(ErrorCode.SELF_ANSWER_NOT_ALLOWED);
        }
        // 1인 1답 제한
        if (answerRepository.existsByQnaIdAndAuthorId(qnaId, memberId)) {
            throw new CustomException(ErrorCode.ANSWER_ALREADY_EXISTS);
        }

        Answer answer = Answer.builder()
                .qna(qna)
                .authorId(memberId)
                .isAnonymous(req.isAnonymous())
                .imageUrl(req.imageUrl())
                .build();
        answerRepository.save(answer);

        answerTranslationRepository.save(AnswerTranslation.builder()
                .answer(answer)
                .language(req.language())
                .content(req.content())
                .build());

        translationService.translateAnswer(answer, req.content(), req.language());

        // 익명 답변 시 QNA 컨텍스트에서 alias 할당
        if (req.isAnonymous()) {
            anonymousAliasService.assign(AliasContextType.QNA, qnaId, memberId);
        }

        return answer.getId();
    }

    @Transactional
    public void deleteAnswer(Long answerId, Long memberId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));
        if (!answer.getAuthorId().equals(memberId) && !memberQueryPort.isAdmin(memberId)) {
            throw new CustomException(ErrorCode.ANSWER_UNAUTHORIZED);
        }

        // 답변 좋아요 먼저 삭제 (answer_likes FK)
        answerLikeRepository.deleteByAnswerId(answerId);

        // 답변 삭제 (AnswerTranslation cascade)
        answerRepository.delete(answer);
    }

    /**
     * 답변 채택.
     * - 질문 작성자만 채택 가능
     * - 이미 채택된 질문이면 불가
     * - 채택 시 Answer.isAdopted = true, QnA.isAdopted = true
     */
    @Transactional
    public void adoptAnswer(Long qnaId, Long answerId, Long memberId) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));

        if (!qna.getAuthorId().equals(memberId)) {
            throw new CustomException(ErrorCode.ADOPT_UNAUTHORIZED);
        }
        if (qna.getIsAdopted()) {
            throw new CustomException(ErrorCode.ALREADY_ADOPTED);
        }

        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));

        // 해당 답변이 이 QnA에 속하는지 검증
        if (!answerRepository.existsByIdAndQnaId(answerId, qnaId)) {
            throw new CustomException(ErrorCode.ANSWER_NOT_FOUND);
        }

        answer.adopt();
        qna.adopt();
    }

    @Transactional
    public boolean toggleAnswerLike(Long answerId, Long memberId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));

        if (answerLikeRepository.existsByMemberIdAndAnswerId(memberId, answerId)) {
            answerLikeRepository.deleteByMemberIdAndAnswerId(memberId, answerId);
            answer.decreaseLikeCount();
            return false;
        } else {
            answerLikeRepository.save(AnswerLike.builder().memberId(memberId).answer(answer).build());
            answer.increaseLikeCount();
            return true;
        }
    }

    // ───────── helpers ─────────

    private QnATranslation resolveQnATranslation(QnA qna, Language language, boolean original) {
        if (original) {
            return qnaTranslationRepository.findFirstByQnaIdOrderByIdAsc(qna.getId())
                    .orElseGet(() -> qna.getTranslations().get(0));
        }
        return qnaTranslationRepository.findByQnaIdAndLanguage(qna.getId(), language)
                .orElseGet(() -> qnaTranslationRepository
                        .findByQnaIdAndLanguage(qna.getId(), Language.EN)
                        .orElseGet(() -> qna.getTranslations().get(0)));
    }

    private AnswerResponse buildAnswerResponse(Answer answer, Long qnaId, Long memberId, Language language, boolean original) {
        AnswerTranslation translation = original
                ? answerTranslationRepository.findFirstByAnswerIdOrderByIdAsc(answer.getId())
                        .orElseGet(() -> answer.getTranslations().get(0))
                : answerTranslationRepository
                        .findByAnswerIdAndLanguage(answer.getId(), language)
                        .orElseGet(() -> answerTranslationRepository
                                .findByAnswerIdAndLanguage(answer.getId(), Language.EN)
                                .orElseGet(() -> answer.getTranslations().get(0)));
        // 답변 원문(소스) 행 — 답변마다 원문 언어가 다를 수 있어 함께 내려준다.
        AnswerTranslation source = answerTranslationRepository.findFirstByAnswerIdOrderByIdAsc(answer.getId())
                .orElse(translation);

        String authorName = resolveAuthorName(answer.getIsAnonymous(),
                AliasContextType.QNA, qnaId, answer.getAuthorId());
        boolean isLiked = answerLikeRepository.existsByMemberIdAndAnswerId(memberId, answer.getId());
        boolean isOwner = answer.getAuthorId().equals(memberId);

        return AnswerResponse.of(answer, translation, source, authorName, isLiked, isOwner);
    }

    private String resolveAuthorName(boolean isAnonymous, AliasContextType ctx, Long contextId, Long authorId) {
        if (isAnonymous) {
            return anonymousAliasService.lookup(ctx, contextId, authorId);
        }
        return profileQueryPort.findName(authorId).orElse("Unknown");
    }
}
