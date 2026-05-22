package com.khu.globalhub.characterization;

import com.fasterxml.jackson.databind.JsonNode;
import com.khu.globalhub.AbstractIntegrationTest;
import com.khu.globalhub.global.infra.S3Service;
import com.khu.globalhub.global.infra.TranslationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 채팅 DM(/api/chat) 플로우 현재 동작 박제 (characterization).
 * ChatRoom 없이 sender+receiver 조합으로 대화를 식별한다.
 */
@DisplayName("[characterization] 채팅 플로우")
class ChatFlowTest extends AbstractIntegrationTest {

    // 모든 characterization 클래스가 동일한 @MockitoBean 셋을 선언해 단일 Spring 컨텍스트(=단일 컨테이너)를 공유한다.
    @MockitoBean
    TranslationService translationService;
    @MockitoBean
    S3Service s3Service;

    private long myMemberId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/members/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn();
        return readData(result).get("memberId").asLong();
    }

    private void send(String token, long receiverId, String content) throws Exception {
        mockMvc.perform(post("/api/chat")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("receiverId", receiverId, "content", content)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("자기 자신에게 메시지 전송 시 400 CANNOT_CHAT_WITH_SELF")
    void cannotMessageSelf() throws Exception {
        String token = signUpWithProfile("solo@khu.ac.kr", "솔로");
        long myId = myMemberId(token);

        mockMvc.perform(post("/api/chat")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("receiverId", myId, "content", "혼잣말")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("자기 자신에게 메시지를 보낼 수 없습니다."));
    }

    @Test
    @DisplayName("메시지 전송→대화 목록: 마지막 메시지 + 안 읽은 수 노출")
    void conversationListShowsLastMessageAndUnread() throws Exception {
        String alice = signUpWithProfile("calice@khu.ac.kr", "앨리스");
        String bob = signUpWithProfile("cbob@khu.ac.kr", "밥");
        long aliceId = myMemberId(alice);
        long bobId = myMemberId(bob);

        send(alice, bobId, "안녕");
        send(alice, bobId, "잘 지내?");

        // 밥의 대화 목록 — 앨리스로부터 2건 미읽음, 마지막 메시지="잘 지내?"
        MvcResult list = mockMvc.perform(get("/api/chat")
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode convos = readData(list);
        assertThat(convos).hasSize(1);
        assertThat(convos.get(0).get("partnerId").asLong()).isEqualTo(aliceId);
        assertThat(convos.get(0).get("partnerName").asText()).isEqualTo("앨리스");
        assertThat(convos.get(0).get("lastMessage").asText()).isEqualTo("잘 지내?");
        assertThat(convos.get(0).get("unreadCount").asInt()).isEqualTo(2);
    }

    @Test
    @DisplayName("대화 상세 조회 시 내가 받은 메시지는 자동 읽음 처리 → 이후 unreadCount=0")
    void getConversationMarksRead() throws Exception {
        String alice = signUpWithProfile("dalice@khu.ac.kr", "앨리스D");
        String bob = signUpWithProfile("dbob@khu.ac.kr", "밥D");
        long aliceId = myMemberId(alice);
        long bobId = myMemberId(bob);

        send(alice, bobId, "msg1");
        send(alice, bobId, "msg2");

        // 밥이 대화 상세를 열면 받은 메시지 읽음 처리
        MvcResult convo = mockMvc.perform(get("/api/chat/{partnerId}", aliceId)
                        .header("Authorization", bearer(bob)))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode msgs = readData(convo);
        assertThat(msgs).hasSize(2);
        assertThat(msgs.get(0).get("content").asText()).isEqualTo("msg1");
        assertThat(msgs.get(0).get("senderId").asLong()).isEqualTo(aliceId);
        assertThat(msgs.get(0).get("senderName").asText()).isEqualTo("앨리스D");
        assertThat(msgs.get(0).get("isSystem").asBoolean()).isFalse();
        assertThat(msgs.get(0).get("isRead").asBoolean()).isTrue();

        // 읽음 처리 후 밥 목록의 unreadCount=0
        MvcResult list = mockMvc.perform(get("/api/chat")
                        .header("Authorization", bearer(bob)))
                .andReturn();
        assertThat(readData(list).get(0).get("unreadCount").asInt()).isEqualTo(0);
    }
}
