package com.khu.globalhub.coursereview.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 강의평 엔티티. 익명 노출(작성자 이름 미표시)이지만 author_id는 저장(중복/신고용).
 */
@Entity
@Table(name = "course_reviews")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 강의 ID. */
    @Column(name = "lecture_id", nullable = false)
    private Long lectureId;

    /** 작성자 ID (익명 노출, 신고/중복 방지용). */
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    /** 별점 1~5. */
    @Column(nullable = false)
    private Integer rating;

    /** 강의평 본문. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    // ── 선택 입력 지표 (null 허용, 집계에서 무시) ──

    /** 수업/출석 방식 (대면/비대면/혼합). */
    @Enumerated(EnumType.STRING)
    @Column(name = "attendance_type")
    private AttendanceType attendanceType;

    /** 발표 빈도 (적음/보통/많음). */
    @Enumerated(EnumType.STRING)
    @Column(name = "presentation_freq")
    private FrequencyLevel presentationFreq;

    /** 조모임 빈도 (적음/보통/많음). */
    @Enumerated(EnumType.STRING)
    @Column(name = "group_work_freq")
    private FrequencyLevel groupWorkFreq;

    /** 과제 빈도 (적음/보통/많음). */
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_freq")
    private FrequencyLevel assignmentFreq;

    /** 한국어 사용 빈도 (적음=유학생 친화 / 보통 / 많음). */
    @Enumerated(EnumType.STRING)
    @Column(name = "korean_usage")
    private FrequencyLevel koreanUsage;
}
