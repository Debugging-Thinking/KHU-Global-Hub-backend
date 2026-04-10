package com.khu.globalhub.domain.qna.entity;

import com.khu.globalhub.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "qna_likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "qna_id"})
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class QnALike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "qna_id", nullable = false)
    private QnA qna;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
