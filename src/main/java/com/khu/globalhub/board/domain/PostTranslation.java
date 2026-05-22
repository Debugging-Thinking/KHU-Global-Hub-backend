package com.khu.globalhub.board.domain;

import com.khu.globalhub.shared.enums.Language;
import jakarta.persistence.*;
import lombok.*;

/**
 * 게시글 번역 엔티티.
 * 게시글 1개당 최대 6개 행이 생성된다 (언어별로 1개씩).
 *
 * 예시: post_id=1 인 게시글의 번역 데이터
 *   (1, 1, KO, "제목", "내용")
 *   (2, 1, EN, "Title", "Content")
 *   (3, 1, ZH, "标题", "内容")
 *   ...
 *
 * 조회 시: 유저의 language 설정에 맞는 행 1개만 가져온다.
 */
@Entity
@Table(
    name = "post_translations",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "language"}) // 게시글당 언어 중복 방지
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class PostTranslation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어떤 게시글의 번역인지. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    /** 이 행이 어떤 언어 버전인지. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Language language;

    /** 해당 언어로 번역된 제목. */
    @Column(nullable = false)
    private String title;

    /** 해당 언어로 번역된 본문. TEXT 타입으로 긴 내용도 저장 가능. */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    public void updateContent(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
