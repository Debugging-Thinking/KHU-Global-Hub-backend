package com.khu.globalhub.chat.application;

import com.khu.globalhub.chat.presentation.dto.ChatMessageResponse;
import com.khu.globalhub.chat.presentation.dto.ConversationSummaryResponse;
import com.khu.globalhub.chat.presentation.dto.SendMessageRequest;
import com.khu.globalhub.chat.domain.ChatMessage;
import com.khu.globalhub.chat.infrastructure.ChatMessageRepository;
import com.khu.globalhub.shared.port.MemberQueryPort;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import com.khu.globalhub.shared.exception.CustomException;
import com.khu.globalhub.shared.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MemberQueryPort memberQueryPort;
    private final ProfileQueryPort profileQueryPort;

    /**
     * 메시지 전송.
     */
    @Transactional
    public Long sendMessage(Long senderId, SendMessageRequest req) {
        if (senderId.equals(req.receiverId())) {
            throw new CustomException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        // receiverId는 클라이언트 입력값 → 존재 검증 유지 (senderId는 JWT라 항상 유효)
        if (!memberQueryPort.exists(req.receiverId())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        // 텍스트 또는 이미지 중 하나는 반드시 있어야 한다(이미지만 보내기 허용).
        boolean hasContent = req.content() != null && !req.content().isBlank();
        boolean hasImage = req.imageUrl() != null && !req.imageUrl().isBlank();
        if (!hasContent && !hasImage) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        ChatMessage msg = ChatMessage.builder()
                .senderId(senderId)
                .receiverId(req.receiverId())
                .content(hasContent ? req.content() : "")
                .imageUrl(hasImage ? req.imageUrl() : null)
                .build();
        chatMessageRepository.save(msg);

        return msg.getId();
    }

    /**
     * A-B 대화 내용 조회 + 내가 받은 메시지 읽음 처리.
     */
    @Transactional
    public List<ChatMessageResponse> getConversation(Long myId, Long partnerId) {
        List<ChatMessage> messages = chatMessageRepository.findConversation(myId, partnerId);

        // 내가 수신자인 읽지 않은 메시지 일괄 읽음 처리
        messages.stream()
                .filter(m -> !m.getIsRead()
                        && m.getReceiverId().equals(myId)
                        && !m.getIsSystem())
                .forEach(ChatMessage::markAsRead);

        return messages.stream()
                .map(msg -> {
                    String senderName = null;
                    if (!msg.getIsSystem() && msg.getSenderId() != null) {
                        senderName = profileQueryPort.findName(msg.getSenderId())
                                .orElse("Unknown");
                    }
                    return ChatMessageResponse.of(msg, senderName);
                })
                .toList();
    }

    /**
     * 내 DM 대화 상대 목록 (마지막 메시지 + 안 읽은 수 포함).
     */
    public List<ConversationSummaryResponse> getConversationList(Long myId) {
        List<Long> partnerIds = chatMessageRepository.findPartnerIdsByMemberId(myId);

        List<ConversationSummaryResponse> result = new ArrayList<>();
        for (Long partnerId : partnerIds) {
            if (partnerId == null) continue;

            ProfileQueryPort.ProfileCard partnerCard = profileQueryPort.findCard(partnerId).orElse(null);
            String partnerName = partnerCard != null ? partnerCard.name() : "Unknown";
            String partnerImage = partnerCard != null ? partnerCard.profileImage() : null;

            ChatMessage last = chatMessageRepository.findLastMessage(myId, partnerId).orElse(null);
            if (last == null) continue;

            int unread = chatMessageRepository
                    .countBySenderIdAndReceiverIdAndIsReadFalse(partnerId, myId);

            result.add(new ConversationSummaryResponse(
                    partnerId,
                    partnerName,
                    partnerImage,
                    last.getContent(),
                    unread,
                    last.getSentAt()
            ));
        }

        // 마지막 메시지 최신순 정렬
        result.sort((a, b) -> b.lastMessageAt().compareTo(a.lastMessageAt()));
        return result;
    }
}
