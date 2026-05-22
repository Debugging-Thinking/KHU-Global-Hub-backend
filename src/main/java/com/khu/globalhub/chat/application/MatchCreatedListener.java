package com.khu.globalhub.chat.application;

import com.khu.globalhub.chat.domain.ChatMessage;
import com.khu.globalhub.chat.infrastructure.ChatMessageRepository;
import com.khu.globalhub.shared.extevent.mentoring.MatchCreatedEvent;
import com.khu.globalhub.shared.port.ProfileQueryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * mentoring의 {@link MatchCreatedEvent}를 받아 양쪽에게 매칭 안내 시스템 메시지를 삽입한다.
 * chat BC가 ChatMessage의 소유자 — mentoring은 chat 내부를 모른다(이전엔 직접 INSERT, 경계 위반).
 * 표시 이름은 ProfileQueryPort로 조회. AFTER_COMMIT 동기 실행.
 */
@Component
@RequiredArgsConstructor
public class MatchCreatedListener {

    private final ChatMessageRepository chatMessageRepository;
    private final ProfileQueryPort profileQueryPort;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onMatchCreated(MatchCreatedEvent event) {
        String mentorName = profileQueryPort.findName(event.mentorId()).orElse("Unknown");
        String menteeName = profileQueryPort.findName(event.menteeId()).orElse("Unknown");
        String content = buildSystemMessage(mentorName, menteeName);

        // 멘토에게
        chatMessageRepository.save(ChatMessage.builder()
                .receiverId(event.mentorId())
                .contextPartnerId(event.menteeId())
                .content(content)
                .isSystem(true)
                .build());
        // 멘티에게
        chatMessageRepository.save(ChatMessage.builder()
                .receiverId(event.menteeId())
                .contextPartnerId(event.mentorId())
                .content(content)
                .isSystem(true)
                .build());
    }

    private String buildSystemMessage(String mentorName, String menteeName) {
        return String.format(
                "멘토-멘티 매칭이 완료되었습니다!\n멘토: %s / 멘티: %s\n자유롭게 대화를 시작해보세요.",
                mentorName, menteeName);
    }
}
