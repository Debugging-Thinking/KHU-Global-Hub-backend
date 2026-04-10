package com.khu.globalhub.domain.qna.service;

import com.khu.globalhub.domain.member.entity.Member;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.member.repository.MemberRepository;
import com.khu.globalhub.domain.member.repository.ProfileRepository;
import com.khu.globalhub.domain.qna.dto.*;
import com.khu.globalhub.domain.qna.entity.*;
import com.khu.globalhub.domain.qna.repository.*;
import com.khu.globalhub.global.enums.Language;
import com.khu.globalhub.global.exception.CustomException;
import com.khu.globalhub.global.exception.ErrorCode;
import com.khu.globalhub.global.infra.TranslationService;
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
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;
    private final TranslationService translationService;

    // ───────── QnA CRUD ─────────

    @Transactional
    public Long createQnA(Long memberId, CreateQnARequest req) {
        Member author = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        QnA qna = QnA.builder()
                .author(author)
                .isAnonymous(req.isAnonymous())
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

        return qna.getId();
    }

    public Page<QnASummaryResponse> getQnAList(Language language, Pageable pageable) {
        return qnaRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(qna -> {
                    QnATranslation translation = resolveQnATranslation(qna, language);
                    String authorName = getAuthorName(qna.getAuthor());
                    int answerCount = answerRepository.countByQnaId(qna.getId());
                    return QnASummaryResponse.of(qna, translation, authorName, answerCount);
                });
    }

    public QnADetailResponse getQnA(Long qnaId, Long memberId, Language language) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));

        QnATranslation translation = resolveQnATranslation(qna, language);
        String authorName = getAuthorName(qna.getAuthor());
        boolean isLiked = qnaLikeRepository.existsByMemberIdAndQnaId(memberId, qnaId);
        boolean isOwner = qna.getAuthor().getId().equals(memberId);

        List<AnswerResponse> answers = answerRepository.findByQnaIdOrderByCreatedAtAsc(qnaId)
                .stream()
                .map(answer -> buildAnswerResponse(answer, memberId, language))
                .toList();

        return QnADetailResponse.of(qna, translation, authorName, isLiked, isOwner, answers);
    }

    @Transactional
    public void deleteQnA(Long qnaId, Long memberId) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
        if (!qna.getAuthor().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.QNA_UNAUTHORIZED);
        }
        qnaRepository.delete(qna);
    }

    @Transactional
    public boolean toggleQnALike(Long qnaId, Long memberId) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (qnaLikeRepository.existsByMemberIdAndQnaId(memberId, qnaId)) {
            qnaLikeRepository.deleteByMemberIdAndQnaId(memberId, qnaId);
            qna.decreaseLikeCount();
            return false;
        } else {
            qnaLikeRepository.save(QnALike.builder().member(member).qna(qna).build());
            qna.increaseLikeCount();
            return true;
        }
    }

    // ───────── Answer CRUD ─────────

    @Transactional
    public Long createAnswer(Long qnaId, Long memberId, CreateAnswerRequest req) {
        QnA qna = qnaRepository.findById(qnaId)
                .orElseThrow(() -> new CustomException(ErrorCode.QNA_NOT_FOUND));
        Member author = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        Answer answer = Answer.builder()
                .qna(qna)
                .author(author)
                .isAnonymous(req.isAnonymous())
                .build();
        answerRepository.save(answer);

        answerTranslationRepository.save(AnswerTranslation.builder()
                .answer(answer)
                .language(req.language())
                .content(req.content())
                .build());

        translationService.translateAnswer(answer, req.content(), req.language());

        return answer.getId();
    }

    @Transactional
    public void deleteAnswer(Long answerId, Long memberId) {
        Answer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new CustomException(ErrorCode.ANSWER_NOT_FOUND));
        if (!answer.getAuthor().getId().equals(memberId)) {
            throw new CustomException(ErrorCode.ANSWER_UNAUTHORIZED);
        }
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

        if (!qna.getAuthor().getId().equals(memberId)) {
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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (answerLikeRepository.existsByMemberIdAndAnswerId(memberId, answerId)) {
            answerLikeRepository.deleteByMemberIdAndAnswerId(memberId, answerId);
            answer.decreaseLikeCount();
            return false;
        } else {
            answerLikeRepository.save(AnswerLike.builder().member(member).answer(answer).build());
            answer.increaseLikeCount();
            return true;
        }
    }

    // ───────── helpers ─────────

    private QnATranslation resolveQnATranslation(QnA qna, Language language) {
        return qnaTranslationRepository.findByQnaIdAndLanguage(qna.getId(), language)
                .orElseGet(() -> qnaTranslationRepository
                        .findByQnaIdAndLanguage(qna.getId(), Language.EN)
                        .orElseGet(() -> qna.getTranslations().get(0)));
    }

    private AnswerResponse buildAnswerResponse(Answer answer, Long memberId, Language language) {
        AnswerTranslation translation = answerTranslationRepository
                .findByAnswerIdAndLanguage(answer.getId(), language)
                .orElseGet(() -> answerTranslationRepository
                        .findByAnswerIdAndLanguage(answer.getId(), Language.EN)
                        .orElseGet(() -> answer.getTranslations().get(0)));

        String authorName = getAuthorName(answer.getAuthor());
        boolean isLiked = answerLikeRepository.existsByMemberIdAndAnswerId(memberId, answer.getId());
        boolean isOwner = answer.getAuthor().getId().equals(memberId);

        return AnswerResponse.of(answer, translation, authorName, isLiked, isOwner);
    }

    private String getAuthorName(Member author) {
        return profileRepository.findByMemberId(author.getId())
                .map(Profile::getName)
                .orElse("Unknown");
    }
}
