package com.khu.globalhub.mentoring.activity.domain;

import com.khu.globalhub.shared.common.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mentoring_activities")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MentoringActivity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 매칭 ID */
    @Column(nullable = false)
    private Long matchId;

    /** 작성자 ID */
    @Column(nullable = false)
    private Long authorId;

    /** 제목 */
    @Column(nullable = false)
    private String title;

    /** 내용 */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
}