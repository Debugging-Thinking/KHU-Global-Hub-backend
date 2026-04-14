package com.khu.globalhub.domain.anonymous.entity;

import com.khu.globalhub.global.enums.AliasContextType;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 익명 번호 매핑 테이블.
 * (contextType, contextId) 범위 안에서 memberId에게 순번을 할당한다.
 * - POST: 게시글 단위
 * - QNA: Q&A 단위 (질문, 답변, 댓글 모두 동일 번호 공유)
 */
@Entity
@Table(
        name = "anonymous_aliases",
        uniqueConstraints = @UniqueConstraint(columnNames = {"context_type", "context_id", "member_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AnonymousAlias {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "context_type", nullable = false)
    private AliasContextType contextType;

    @Column(name = "context_id", nullable = false)
    private Long contextId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    /** 이 멤버에게 할당된 순번. 1부터 시작, 게시글/Q&A 내에서 고유. */
    @Column(nullable = false)
    private Integer aliasNumber;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
