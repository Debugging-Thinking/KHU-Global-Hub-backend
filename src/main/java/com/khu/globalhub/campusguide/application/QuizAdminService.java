package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.domain.QuizQuestion;
import com.khu.globalhub.campusguide.domain.QuizQuestionTranslation;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionRepository;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionTranslationRepository;
import com.khu.globalhub.campusguide.presentation.dto.AdminQuizQuestionRequest;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 관리자 전용 퀴즈 문항 CRUD 유스케이스.
 * 생성/수정 시 메타(category/answerIndex) 저장 + 원문(KO 등) 번역 행 동기 저장 + 나머지 언어 비동기 번역.
 * 게시판/강의평 작성 흐름과 동일한 사전번역 패턴을 재사용한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizAdminService {

    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionTranslationRepository translationRepository;
    private final QuizTranslationWriter translationWriter;

    @Transactional
    public Long create(AdminQuizQuestionRequest req) {
        QuizQuestion question = questionRepository.save(QuizQuestion.builder()
                .category(req.category())
                .answerIndex(req.answerIndex())
                .build());

        Language claimed = req.language() != null ? req.language() : Language.KO;
        translationRepository.save(QuizQuestionTranslation.builder()
                .questionId(question.getId())
                .language(claimed)
                .question(req.question())
                .optionsJson(QuizOptionsCodec.serialize(req.options()))
                .explanation(req.explanation())
                .build());

        // 나머지 5개 언어 비동기 번역
        translationWriter.translate(question.getId(), req.question(), req.options(), req.explanation(), claimed);
        return question.getId();
    }

    @Transactional
    public void update(Long questionId, AdminQuizQuestionRequest req) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new CustomException(ErrorCode.QUIZ_QUESTION_NOT_FOUND));
        question.updateMeta(req.category(), req.answerIndex());

        Language claimed = req.language() != null ? req.language() : Language.KO;
        String optionsJson = QuizOptionsCodec.serialize(req.options());

        // 원문(작성자 언어) 행 upsert (게시글 PUT과 동일)
        translationRepository.findByQuestionIdAndLanguage(questionId, claimed)
                .ifPresentOrElse(
                        row -> { row.updateContent(req.question(), optionsJson, req.explanation()); translationRepository.save(row); },
                        () -> translationRepository.save(QuizQuestionTranslation.builder()
                                .questionId(questionId).language(claimed)
                                .question(req.question()).optionsJson(optionsJson).explanation(req.explanation())
                                .build()));

        // 나머지 언어 재번역(upsert)
        translationWriter.translate(questionId, req.question(), req.options(), req.explanation(), claimed);
    }

    @Transactional
    public void delete(Long questionId) {
        if (!questionRepository.existsById(questionId)) {
            throw new CustomException(ErrorCode.QUIZ_QUESTION_NOT_FOUND);
        }
        translationRepository.deleteByQuestionId(questionId);
        questionRepository.deleteById(questionId);
    }
}
