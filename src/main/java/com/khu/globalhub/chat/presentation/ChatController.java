package com.khu.globalhub.chat.presentation;

import com.khu.globalhub.chat.presentation.dto.ChatMessageResponse;
import com.khu.globalhub.chat.presentation.dto.ConversationSummaryResponse;
import com.khu.globalhub.chat.presentation.dto.SendMessageRequest;
import com.khu.globalhub.chat.application.ChatService;
import com.khu.globalhub.shared.common.ApiResponse;
import com.khu.globalhub.shared.util.SecurityUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * 메시지 전송.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> sendMessage(
            @Valid @RequestBody SendMessageRequest request
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        Long msgId = chatService.sendMessage(myId, request);
        return ResponseEntity.ok(ApiResponse.ok("메시지가 전송되었습니다.", msgId));
    }

    /**
     * 특정 상대와의 대화 내용 조회.
     * 조회 시 내가 받은 메시지는 읽음 처리됨.
     */
    @GetMapping("/{partnerId}")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getConversation(
            @PathVariable Long partnerId
    ) {
        Long myId = SecurityUtil.getCurrentMemberId();
        List<ChatMessageResponse> result = chatService.getConversation(myId, partnerId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /**
     * 내 DM 대화 상대 목록 (홈 화면).
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ConversationSummaryResponse>>> getConversationList() {
        Long myId = SecurityUtil.getCurrentMemberId();
        List<ConversationSummaryResponse> result = chatService.getConversationList(myId);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    /** 메시지 삭제 (본인이 보낸 메시지만). */
    @DeleteMapping("/{messageId}")
    public ResponseEntity<ApiResponse<Void>> deleteMessage(@PathVariable Long messageId) {
        Long myId = SecurityUtil.getCurrentMemberId();
        chatService.deleteMessage(myId, messageId);
        return ResponseEntity.ok(ApiResponse.ok("메시지가 삭제되었습니다."));
    }
}
