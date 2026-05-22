package com.khu.globalhub.campusguide.application;

import com.khu.globalhub.campusguide.presentation.dto.MyQuizResultResponse;
import com.khu.globalhub.campusguide.presentation.dto.QuizQuestionResponse;
import com.khu.globalhub.campusguide.presentation.dto.QuizSubmitRequest;
import com.khu.globalhub.campusguide.presentation.dto.QuizSubmitResponse;
import com.khu.globalhub.campusguide.domain.QuizQuestion;
import com.khu.globalhub.campusguide.domain.QuizResult;
import com.khu.globalhub.campusguide.infrastructure.QuizQuestionRepository;
import com.khu.globalhub.campusguide.infrastructure.QuizResultRepository;
import com.khu.globalhub.shared.extevent.campusguide.QuizCompletedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuizService {

    private final QuizQuestionRepository questionRepository;
    private final QuizResultRepository resultRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** 전체 문제 조회 (정답 미포함). 카테고리 필터 선택 가능. */
    public List<QuizQuestionResponse> getQuestions(String category) {
        List<QuizQuestion> questions = (category != null && !category.isBlank())
                ? questionRepository.findByCategory(category)
                : questionRepository.findAll();
        return questions.stream().map(QuizQuestionResponse::from).toList();
    }

    /** 답안 제출 → 채점 → QuizResult 저장 → Profile.quizScore 갱신. */
    @Transactional
    public QuizSubmitResponse submitQuiz(Long memberId, QuizSubmitRequest request) {
        Map<Long, QuizQuestion> questionMap = questionRepository
                .findAllById(request.answers().stream().map(QuizSubmitRequest.QuizAnswerItem::questionId).toList())
                .stream().collect(Collectors.toMap(QuizQuestion::getId, q -> q));

        List<QuizSubmitResponse.QuizAnswerResult> results = new ArrayList<>();
        int correctCount = 0;

        for (QuizSubmitRequest.QuizAnswerItem answer : request.answers()) {
            QuizQuestion question = questionMap.get(answer.questionId());
            if (question == null) continue;

            boolean correct = question.getAnswerIndex() == answer.selectedOption();
            if (correct) correctCount++;

            results.add(new QuizSubmitResponse.QuizAnswerResult(
                    question.getId(), correct, question.getAnswerIndex(), question.getExplanation()
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
}
