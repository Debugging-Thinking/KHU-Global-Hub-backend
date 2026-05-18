package com.khu.globalhub.domain.quiz.controller;

import com.khu.globalhub.domain.quiz.dto.MyQuizResultResponse;
import com.khu.globalhub.domain.quiz.dto.QuizQuestionResponse;
import com.khu.globalhub.domain.quiz.dto.QuizSubmitRequest;
import com.khu.globalhub.domain.quiz.dto.QuizSubmitResponse;
import com.khu.globalhub.domain.quiz.service.QuizService;
import com.khu.globalhub.global.common.ApiResponse;
import com.khu.globalhub.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /** 문제 목록 조회. category 파라미터로 필터링 가능 (생략 시 전체). */
    @GetMapping("/questions")
    public ResponseEntity<ApiResponse<List<QuizQuestionResponse>>> getQuestions(
            @RequestParam(required = false) String category
    ) {
        return ResponseEntity.ok(ApiResponse.ok(quizService.getQuestions(category)));
    }

    /** 답안 제출 → 채점 결과 반환. */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<QuizSubmitResponse>> submit(
            @RequestBody QuizSubmitRequest request
    ) {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok("채점이 완료되었습니다.", quizService.submitQuiz(memberId, request)));
    }

    /** 내 퀴즈 히스토리 조회. */
    @GetMapping("/results/me")
    public ResponseEntity<ApiResponse<List<MyQuizResultResponse>>> getMyResults() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok(quizService.getMyResults(memberId)));
    }

    /** 내 최고 점수 조회. */
    @GetMapping("/score/me")
    public ResponseEntity<ApiResponse<Double>> getMyBestScore() {
        Long memberId = SecurityUtil.getCurrentMemberId();
        return ResponseEntity.ok(ApiResponse.ok(quizService.getMyBestScore(memberId)));
    }
}
