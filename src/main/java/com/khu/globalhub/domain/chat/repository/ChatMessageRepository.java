package com.khu.globalhub.domain.chat.repository;

import com.khu.globalhub.domain.chat.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * A-B 1:1 대화 전체 메시지 조회 (시스템 메시지 포함).
     *
     * 일반 DM: (sender=A, receiver=B) OR (sender=B, receiver=A)
     * 시스템 메시지: (isSystem=true, receiver=A, contextPartner=B)
     *              OR (isSystem=true, receiver=B, contextPartner=A)
     */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE (m.sender.id = :aId AND m.receiver.id = :bId)
           OR (m.sender.id = :bId AND m.receiver.id = :aId)
           OR (m.isSystem = true AND m.receiver.id = :aId AND m.contextPartner.id = :bId)
           OR (m.isSystem = true AND m.receiver.id = :bId AND m.contextPartner.id = :aId)
        ORDER BY m.sentAt ASC
        """)
    List<ChatMessage> findConversation(@Param("aId") Long aId, @Param("bId") Long bId);

    /**
     * 읽지 않은 메시지 수 조회 (배지 표시용).
     */
    int countBySenderIdAndReceiverIdAndIsReadFalse(Long senderId, Long receiverId);

    /**
     * 내가 참여한 대화 상대 ID 목록.
     * sender 또는 receiver로 등장한 상대방 ID를 중복 없이 수집.
     * 시스템 메시지(isSystem=true)는 contextPartner로 상대방을 식별.
     */
    @Query("""
        SELECT DISTINCT
            CASE
                WHEN m.sender.id = :memberId THEN m.receiver.id
                WHEN m.receiver.id = :memberId AND m.isSystem = false THEN m.sender.id
                WHEN m.receiver.id = :memberId AND m.isSystem = true THEN m.contextPartner.id
            END
        FROM ChatMessage m
        WHERE m.sender.id = :memberId
           OR (m.receiver.id = :memberId AND m.isSystem = false)
           OR (m.receiver.id = :memberId AND m.isSystem = true)
        """)
    List<Long> findPartnerIdsByMemberId(@Param("memberId") Long memberId);

    /**
     * 특정 두 사람의 마지막 메시지 조회 (대화 목록 미리보기용).
     */
    @Query("""
        SELECT m FROM ChatMessage m
        WHERE ((m.sender.id = :aId AND m.receiver.id = :bId)
            OR (m.sender.id = :bId AND m.receiver.id = :aId)
            OR (m.isSystem = true AND m.receiver.id = :aId AND m.contextPartner.id = :bId)
            OR (m.isSystem = true AND m.receiver.id = :bId AND m.contextPartner.id = :aId))
        ORDER BY m.sentAt DESC
        LIMIT 1
        """)
    Optional<ChatMessage> findLastMessage(@Param("aId") Long aId, @Param("bId") Long bId);
}
