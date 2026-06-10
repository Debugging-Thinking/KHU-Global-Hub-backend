package com.khu.globalhub.coursereview.application;

import com.khu.globalhub.coursereview.domain.AttendanceType;
import com.khu.globalhub.coursereview.domain.CourseReview;
import com.khu.globalhub.coursereview.domain.CourseReviewTranslation;
import com.khu.globalhub.coursereview.domain.FrequencyLevel;
import com.khu.globalhub.coursereview.domain.Lecture;
import com.khu.globalhub.coursereview.infrastructure.CourseReviewRepository;
import com.khu.globalhub.coursereview.infrastructure.CourseReviewTranslationRepository;
import com.khu.globalhub.coursereview.infrastructure.LectureRepository;
import com.khu.globalhub.coursereview.presentation.dto.CourseReviewResponse;
import com.khu.globalhub.coursereview.presentation.dto.CreateReviewRequest;
import com.khu.globalhub.coursereview.presentation.dto.IndicatorSummary;
import com.khu.globalhub.coursereview.presentation.dto.LectureDetailResponse;
import com.khu.globalhub.coursereview.presentation.dto.LectureSummaryResponse;
import com.khu.globalhub.shared.enums.Language;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import com.khu.globalhub.shared.port.MemberQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 강의평 유스케이스 — 강의 검색/상세/리뷰 작성·삭제. (강의평 본문은 익명 노출)
 * 강의평 본문은 게시판과 동일하게 6개 언어 사전번역 + 원문 토글.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CourseReviewService {

    private final LectureRepository lectureRepository;
    private final MemberQueryPort memberQueryPort;
    private final CourseReviewRepository reviewRepository;
    private final CourseReviewTranslationRepository translationRepository;
    private final CourseReviewTranslationWriter translationWriter;

    public Page<LectureSummaryResponse> search(String semester, String query, Pageable pageable) {
        String q = query == null ? "" : query.trim();
        return lectureRepository.search(semester, q, pageable)
                .map(l -> LectureSummaryResponse.of(l,
                        reviewRepository.countByLectureId(l.getId()),
                        reviewRepository.avgRating(l.getId())));
    }

    public LectureDetailResponse getLecture(Long lectureId, Long memberId, Language language, boolean original) {
        Lecture lecture = lectureRepository.findById(lectureId)
                .orElseThrow(() -> new CustomException(ErrorCode.LECTURE_NOT_FOUND));
        List<CourseReview> entities = reviewRepository.findByLectureIdOrderByCreatedAtDesc(lectureId);

        // 모든 강의평의 번역을 한 번에 조회 후 reviewId로 그룹핑 (N+1 방지)
        List<Long> ids = entities.stream().map(CourseReview::getId).toList();
        Map<Long, List<CourseReviewTranslation>> byReview = ids.isEmpty() ? Map.of()
                : translationRepository.findByReviewIdIn(ids).stream()
                        .collect(Collectors.groupingBy(CourseReviewTranslation::getReviewId));

        List<CourseReviewResponse> reviews = entities.stream().map(r -> {
            List<CourseReviewTranslation> trs = byReview.getOrDefault(r.getId(), List.of());
            CourseReviewTranslation origRow = trs.stream().min(Comparator.comparing(CourseReviewTranslation::getId)).orElse(null);
            CourseReviewTranslation picked = original ? origRow : pick(trs, language, origRow);

            String content = picked != null ? picked.getContent() : r.getContent();
            String originalContent = origRow != null ? origRow.getContent() : r.getContent();
            Language originalLanguage = origRow != null ? origRow.getLanguage() : Language.KO;
            return CourseReviewResponse.of(r, content, originalContent, originalLanguage,
                    r.getAuthorId().equals(memberId));
        }).toList();

        return LectureDetailResponse.of(lecture, reviews.size(),
                reviewRepository.avgRating(lectureId), buildIndicatorSummary(entities), reviews);
    }

    /** 요청 언어 → EN → 원문 행 순으로 번역 행 선택. */
    private CourseReviewTranslation pick(List<CourseReviewTranslation> trs, Language language, CourseReviewTranslation origRow) {
        return trs.stream().filter(t -> t.getLanguage() == language).findFirst()
                .or(() -> trs.stream().filter(t -> t.getLanguage() == Language.EN).findFirst())
                .orElse(origRow);
    }

    @Transactional
    public Long createReview(Long lectureId, Long memberId, CreateReviewRequest req) {
        if (!lectureRepository.existsById(lectureId)) {
            throw new CustomException(ErrorCode.LECTURE_NOT_FOUND);
        }
        CourseReview review = CourseReview.builder()
                .lectureId(lectureId)
                .authorId(memberId)
                .rating(req.rating())
                .content(req.content())
                .attendanceType(req.attendanceType())
                .presentationFreq(req.presentationFreq())
                .groupWorkFreq(req.groupWorkFreq())
                .assignmentFreq(req.assignmentFreq())
                .koreanUsage(req.koreanUsage())
                .build();
        reviewRepository.save(review);

        // 원문 행 동기 저장 → 나머지 6개 언어 비동기 번역 (게시판 방식)
        Language claimed = req.language() != null ? req.language() : Language.KO;
        translationRepository.save(CourseReviewTranslation.builder()
                .reviewId(review.getId()).language(claimed).content(req.content()).build());
        translationWriter.translate(review.getId(), req.content(), claimed);

        return review.getId();
    }

    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        CourseReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new CustomException(ErrorCode.REVIEW_NOT_FOUND));
        if (!review.getAuthorId().equals(memberId) && !memberQueryPort.isAdmin(memberId)) {
            throw new CustomException(ErrorCode.REVIEW_UNAUTHORIZED);
        }
        reviewRepository.delete(review);
    }

    /** 리뷰별 지표를 옵션별 카운트로 집계(0 버킷 포함, null 제외). */
    private IndicatorSummary buildIndicatorSummary(List<CourseReview> reviews) {
        Map<String, Integer> attendance   = zeroBuckets(AttendanceType.values());
        Map<String, Integer> presentation = zeroBuckets(FrequencyLevel.values());
        Map<String, Integer> groupWork    = zeroBuckets(FrequencyLevel.values());
        Map<String, Integer> assignment   = zeroBuckets(FrequencyLevel.values());
        Map<String, Integer> koreanUsage  = zeroBuckets(FrequencyLevel.values());
        for (CourseReview r : reviews) {
            count(attendance, r.getAttendanceType());
            count(presentation, r.getPresentationFreq());
            count(groupWork, r.getGroupWorkFreq());
            count(assignment, r.getAssignmentFreq());
            count(koreanUsage, r.getKoreanUsage());
        }
        return new IndicatorSummary(attendance, presentation, groupWork, assignment, koreanUsage);
    }

    private Map<String, Integer> zeroBuckets(Enum<?>[] values) {
        Map<String, Integer> m = new LinkedHashMap<>();
        for (Enum<?> v : values) m.put(v.name(), 0);
        return m;
    }

    private void count(Map<String, Integer> bucket, Enum<?> value) {
        if (value != null) bucket.merge(value.name(), 1, Integer::sum);
    }
}
