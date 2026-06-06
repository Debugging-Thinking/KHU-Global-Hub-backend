package com.khu.globalhub.coursereview.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * 강의평 번역 엔티티 — 게시판/댓글과 동일한 사전번역 방식.
 * 강의평 1개당 최대 6개 행(언어별 1개). 작성 시 원문 행 동기 저장 + 나머지 비동기 번역.
 */
@Entity
@Table(
    name = "course_review_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "language"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class CourseReviewTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public void updateContent(String content) {
        this.content = content;
    }

    /** 원문 행을 자동 감지된 언어로 재라벨 (작성 시 claimed 언어와 다를 때). */
    public void updateLanguage(Language language) {
        this.language = language;
    }
}
