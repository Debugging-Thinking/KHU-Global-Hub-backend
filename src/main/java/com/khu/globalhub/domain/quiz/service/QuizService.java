package com.khu.globalhub.domain.quiz.service;

import com.khu.globalhub.domain.member.entity.Member;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.member.repository.MemberRepository;
import com.khu.globalhub.domain.member.repository.ProfileRepository;
import com.khu.globalhub.domain.quiz.dto.MyQuizResultResponse;
import com.khu.globalhub.domain.quiz.dto.QuizQuestionResponse;
import com.khu.globalhub.domain.quiz.dto.QuizSubmitRequest;
import com.khu.globalhub.domain.quiz.dto.QuizSubmitResponse;
import com.khu.globalhub.domain.quiz.entity.QuizQuestion;
import com.khu.globalhub.domain.quiz.entity.QuizResult;
import com.khu.globalhub.domain.quiz.repository.QuizQuestionRepository;
import com.khu.globalhub.domain.quiz.repository.QuizResultRepository;
import com.khu.globalhub.global.exception.CustomException;
import com.khu.globalhub.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
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
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;

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
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

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
                .member(member)
                .correctCount(correctCount)
                .totalCount(total)
                .score(score)
                .build());

        // 최고 점수를 프로필에 반영
        profileRepository.findByMemberId(memberId).ifPresent(profile -> {
            if (score > profile.getQuizScore()) {
                profile.updateQuizScore(score);
            }
        });

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
