package com.khu.globalhub.domain.board.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 첨부 이미지 엔티티.
 * 이미지 파일 자체는 AWS S3에 저장하고, 여기에는 S3 URL만 저장한다.
 * 게시글 1개에 여러 이미지가 첨부될 수 있으며, orderIndex로 순서를 관리한다.
 */
@Entity
@Table(name = "post_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이미지가 속한 게시글. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** AWS S3에 업로드된 이미지의 URL. */
    @Column(nullable = false)
    private String imageUrl;

    /** 이미지 표시 순서. 0부터 시작. */
    @Column(nullable = false)
    private Integer orderIndex;
}
