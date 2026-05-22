package com.khu.globalhub.domain.chat.service;

import com.khu.globalhub.domain.chat.dto.ChatMessageResponse;
import com.khu.globalhub.domain.chat.dto.ConversationSummaryResponse;
import com.khu.globalhub.domain.chat.dto.SendMessageRequest;
import com.khu.globalhub.domain.chat.entity.ChatMessage;
import com.khu.globalhub.domain.chat.repository.ChatMessageRepository;
import com.khu.globalhub.domain.member.entity.Profile;
import com.khu.globalhub.domain.member.repository.MemberRepository;
import com.khu.globalhub.domain.member.repository.ProfileRepository;
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
    private final MemberRepository memberRepository;
    private final ProfileRepository profileRepository;

    /**
     * 메시지 전송.
     */
    @Transactional
    public Long sendMessage(Long senderId, SendMessageRequest req) {
        if (senderId.equals(req.receiverId())) {
            throw new CustomException(ErrorCode.CANNOT_CHAT_WITH_SELF);
        }

        // receiverId는 클라이언트 입력값 → 존재 검증 유지 (senderId는 JWT라 항상 유효)
        if (!memberRepository.existsById(req.receiverId())) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        ChatMessage msg = ChatMessage.builder()
                .senderId(senderId)
                .receiverId(req.receiverId())
                .content(req.content())
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
                        senderName = profileRepository.findByMemberId(msg.getSenderId())
                                .map(Profile::getName)
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

            Profile partnerProfile = profileRepository.findByMemberId(partnerId).orElse(null);
            String partnerName = partnerProfile != null ? partnerProfile.getName() : "Unknown";
            String partnerImage = partnerProfile != null ? partnerProfile.getProfileImage() : null;

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
