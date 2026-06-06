package com.khu.globalhub.chat.domain;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 1:1 DM 메시지 엔티티.
 *
 * 일반 메시지: sender → receiver 방향의 일반 대화.
 * 시스템 메시지: sender = NULL, isSystem = true.
 *   → 멘토-멘티 매칭 시 자동 삽입: "멘토-멘티 매칭되었습니다. 자유롭게 대화하세요!"
 *   → contextPartner를 함께 저장해 어느 대화에 속하는지 식별한다.
 *
 * A-B 대화 조회 쿼리:
 *   (sender=A AND receiver=B)
 *   OR (sender=B AND receiver=A)
 *   OR (isSystem=true AND receiver=A AND contextPartner=B)
 *   OR (isSystem=true AND receiver=B AND contextPartner=A)
 *
 * 폴링 방식: 프론트에서 주기적으로 GET /api/chat/{userId}/messages 호출.
 */
@Entity
@Table(name = "chat_messages")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 메시지 발신자 ID. identity BC를 ID로만 참조 (D7).
     * 시스템 메시지(isSystem=true)인 경우 NULL.
     */
    @Column(name = "sender_id")
    private Long senderId;

    /**
     * 메시지 수신자 ID.
     * 일반 DM: 실제 수신 유저.
     * 시스템 메시지: 두 유저 중 한 명 (contextPartnerId와 함께 대화 식별에 사용).
     */
    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    /**
     * 시스템 메시지 전용 대화 상대 식별용 ID.
     * 일반 DM은 NULL.
     * 시스템 메시지일 때 receiverId와 함께 이 메시지가 속한 대화를 특정한다.
     */
    @Column(name = "context_partner_id")
    private Long contextPartnerId;

    /** 메시지 내용. (이미지만 보낼 경우 빈 문자열) */
    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /** 첨부 이미지 URL (선택). */
    @Column(name = "image_url")
    private String imageUrl;

    /**
     * 시스템 자동 생성 메시지 여부.
     * true: 멘토-멘티 매칭 시 자동 삽입되는 안내 메시지.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    /** 수신자가 메시지를 읽었는지 여부. */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime sentAt;

    public void markAsRead() {
        this.isRead = true;
    }
}
