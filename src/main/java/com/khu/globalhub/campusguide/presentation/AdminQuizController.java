package com.khu.globalhub.campusguide.presentation;

import com.khu.globalhub.campusguide.application.QuizAdminService;
import com.khu.globalhub.campusguide.application.QuizService;
import com.khu.globalhub.campusguide.presentation.dto.AdminQuizQuestionRequest;
import com.khu.globalhub.campusguide.presentation.dto.AdminQuizQuestionResponse;
import com.khu.globalhub.shared.common.AdminGuard;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.enums.Language;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자 전용 퀴즈 문항 운영 API. 모든 엔드포인트 AdminGuard로 관리자만.
 * 생성/수정 시 원문 저장 + 6개 언어 비동기 사전번역(게시판 방식).
 */
@RestController
@RequestMapping("/api/admin/quiz/questions")
@RequiredArgsConstructor
public class AdminQuizController {

    private final QuizAdminService quizAdminService;
    private final QuizService quizService;
    private final AdminGuard adminGuard;

    /** 관리자 문항 목록 (정답/해설 포함 — 수정 폼 프리필용). */
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdminQuizQuestionResponse>>> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Language language
    ) {
        adminGuard.check();
        return ResponseEntity.ok(ApiResponse.ok("ok",
                quizService.getQuestionsForAdmin(category, language != null ? language : Language.KO)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Long>> create(@RequestBody AdminQuizQuestionRequest request) {
        adminGuard.check();
        return ResponseEntity.ok(ApiResponse.ok("퀴즈 문항을 생성했습니다.", quizAdminService.create(request)));
    }

    @PutMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> update(
            @PathVariable Long questionId,
            @RequestBody AdminQuizQuestionRequest request
    ) {
        adminGuard.check();
        quizAdminService.update(questionId, request);
        return ResponseEntity.ok(ApiResponse.ok("퀴즈 문항을 수정했습니다.", null));
    }

    @DeleteMapping("/{questionId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long questionId) {
        adminGuard.check();
        quizAdminService.delete(questionId);
        return ResponseEntity.ok(ApiResponse.ok("퀴즈 문항을 삭제했습니다.", null));
    }
}
