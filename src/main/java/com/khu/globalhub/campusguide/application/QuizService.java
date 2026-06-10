package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.presentation.dto.AdminQuizQuestionResponse;
import com.khu.globalhub.campusguide.presentation.dto.MyQuizResultResponse;
import com.khu.globalhub.campusguide.presentation.dto.QuizQuestionResponse;
import com.khu.globalhub.campusguide.presentation.dto.QuizSubmitRequest;
import com.khu.globalhub.campusguide.presentation.dto.QuizSubmitResponse;
import com.khu.globalhub.campusguide.domain.BadgeId;
import com.khu.globalhub.campusguide.domain.QuizQuestion;
import com.khu.globalhub.campusguide.domain.QuizQuestionTranslation;
import com.khu.globalhub.campusguide.domain.QuizResult;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionRepository;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionTranslationRepository;
import com.khu.globalhub.campusguide.infrastructure.QuizResultRepository;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.extevent.campusguide.QuizCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizQuestionTranslationRepository translationRepository;
    private final QuizResultRepository resultRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 전체 문제 조회 (정답 미포함). 카테고리 필터 선택 가능.
     * 문항 텍스트는 요청 언어 번역 행으로 조립(없으면 KO → 첫 행 폴백).
     */
    public List<QuizQuestionResponse> getQuestions(BadgeId category, Language language) {
        List<QuizQuestion> questions = (category != null)
                ? questionRepository.findByCategory(category)
                : questionRepository.findAll();

        Map<Long, List<QuizQuestionTranslation>> byQuestion = translationsByQuestion(
                questions.stream().map(QuizQuestion::getId).toList());

        return questions.stream().map(q -> {
            QuizQuestionTranslation picked = pick(byQuestion.getOrDefault(q.getId(), List.of()), language);
            String question = picked != null ? picked.getQuestion() : "";
            List<String> options = picked != null ? QuizOptionsCodec.deserialize(picked.getOptionsJson()) : List.of();
            return new QuizQuestionResponse(q.getId(), q.getCategory().name(), question, options);
        }).toList();
    }

    /**
     * 관리자 전용 문항 조회 (정답 answerIndex + 해설 explanation 포함).
     * 수정 폼 프리필용 — 공개 {@link #getQuestions}와 달리 정답/해설을 내려준다 (AdminGuard 뒤에서만 호출).
     */
    public List<AdminQuizQuestionResponse> getQuestionsForAdmin(BadgeId category, Language language) {
        List<QuizQuestion> questions = (category != null)
                ? questionRepository.findByCategory(category)
                : questionRepository.findAll();

        Map<Long, List<QuizQuestionTranslation>> byQuestion = translationsByQuestion(
                questions.stream().map(QuizQuestion::getId).toList());

        return questions.stream().map(q -> {
            QuizQuestionTranslation picked = pick(byQuestion.getOrDefault(q.getId(), List.of()), language);
            String question = picked != null ? picked.getQuestion() : "";
            List<String> options = picked != null ? QuizOptionsCodec.deserialize(picked.getOptionsJson()) : List.of();
            String explanation = picked != null ? picked.getExplanation() : "";
            return new AdminQuizQuestionResponse(
                    q.getId(), q.getCategory().name(), question, options, q.getAnswerIndex(), explanation);
        }).toList();
    }

    /** 답안 제출 → 채점(answerIndex 기준) → QuizResult 저장 → Profile.quizScore 갱신. */
    @Transactional
    public QuizSubmitResponse submitQuiz(Long memberId, QuizSubmitRequest request) {
        List<Long> questionIds = request.answers().stream()
                .map(QuizSubmitRequest.QuizAnswerItem::questionId).toList();
        Map<Long, QuizQuestion> questionMap = questionRepository.findAllById(questionIds)
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        // 해설(explanation)은 요청 언어 번역 행에서 (없으면 KO 폴백)
        Language language = request.language() != null ? request.language() : Language.KO;
        Map<Long, List<QuizQuestionTranslation>> byQuestion = translationsByQuestion(questionIds);

        List<QuizSubmitResponse.QuizAnswerResult> results = new ArrayList<>();
        int correctCount = 0;

        for (QuizSubmitRequest.QuizAnswerItem answer : request.answers()) {
            QuizQuestion question = questionMap.get(answer.questionId());
            if (question == null) continue;

            boolean correct = question.getAnswerIndex() == answer.selectedOption();
            if (correct) correctCount++;

            QuizQuestionTranslation tr = pick(byQuestion.getOrDefault(question.getId(), List.of()), language);
            String explanation = tr != null ? tr.getExplanation() : null;

            results.add(new QuizSubmitResponse.QuizAnswerResult(
                    question.getId(), correct, question.getAnswerIndex(), explanation
            ));
        }

        int total = results.size();
        double score = total > 0 ? Math.round((double) correctCount / total * 1000.0) / 10.0 : 0.0;

        resultRepository.save(QuizResult.builder()
                .memberId(memberId)
                .correctCount(correctCount)
                .totalCount(total)
                .score(score)
                .build());

        // 최고 점수 반영은 profile BC가 담당 — 이벤트 발행으로 위임 (campusguide는 profile을 모름)
        eventPublisher.publishEvent(new QuizCompletedEvent(memberId, score));

        return new QuizSubmitResponse(correctCount, total, score, results);
    }

    /** 내 퀴즈 결과 히스토리 조회. */
    public List<MyQuizResultResponse> getMyResults(Long memberId) {
        return resultRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                .stream().map(MyQuizResultResponse::from).toList();
    }

    /** 내 현재 최고 점수 조회. */
    public double getMyBestScore(Long memberId) {
        return resultRepository.findTopByMemberIdOrderByScoreDesc(memberId)
                .map(QuizResult::getScore)
                .orElse(0.0);
    }

    /** 여러 문항의 번역 행을 한 번에 조회 후 questionId로 그룹핑 (N+1 방지). */
    private Map<Long, List<QuizQuestionTranslation>> translationsByQuestion(List<Long> questionIds) {
        if (questionIds.isEmpty()) return Map.of();
        return translationRepository.findByQuestionIdIn(questionIds).stream()
                .collect(Collectors.groupingBy(QuizQuestionTranslation::getQuestionId));
    }

    /** 요청 언어 → KO → 첫 행(원문) 순으로 번역 행 선택. */
    private QuizQuestionTranslation pick(List<QuizQuestionTranslation> trs, Language language) {
        return trs.stream().filter(t -> t.getLanguage() == language).findFirst()
                .or(() -> trs.stream().filter(t -> t.getLanguage() == Language.KO).findFirst())
                .or(() -> trs.stream().min(Comparator.comparing(QuizQuestionTranslation::getId)))
                .orElse(null);
    }
}
