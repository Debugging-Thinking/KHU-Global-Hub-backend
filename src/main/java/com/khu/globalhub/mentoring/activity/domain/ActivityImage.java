package com.khu.globalhub.mentoring.activity.domain;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "activity_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ActivityImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private MentoringActivity activity;

    /** S3 URL 또는 Base64 data URI (로컬 환경) */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Column(nullable = false)
    private int orderIndex;
}