package org.nextme.notificationservice.infrastructure.slack;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.nextme.notificationservice.domain.MessageSend;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Slack Web API를 호출해 DM/그룹 DM을 보내는 구현체
 *
 * - conversations.open 으로 채널을 연 뒤
 * - chat.postMessage 로 메시지를 보냄
 */
@Slf4j
@Service
@RefreshScope // config-server 사용 시 /actuator/refresh로 토큰 변경 반영 가능
public class SlackMessageSend implements MessageSend {

    /**
     * Slack Bot User OAuth Token
     * application-*.yml 의 slack.token 에서 주입
     */
    @Value("${slack.token}")
    private String token;

    /**
     * 공용 RestClient (Spring 6 / Boot 3 전용 HTTP 클라이언트)
     */
    private final RestClient client = RestClient.builder()
            .baseUrl("https://slack.com/api")
            .build();

    @Override
    public boolean send(List<String> ids, String message) {
        // 유저 ID가 없으면 전송 불가
        if (ids == null || ids.isEmpty()) {
            log.warn("No slack ids provided");
            return false;
        }

        try {
            // ID가 채널 ID(C로 시작)인지 사용자 ID(U로 시작)인지 확인
            String firstId = ids.get(0);
            String channelId;

            if (firstId.startsWith("C")) {
                // 채널 ID를 직접 사용
                log.info("Using channel ID directly: {}", firstId);
                channelId = firstId;
            } else {
                // 사용자 ID인 경우 DM 채널 열기
                log.info("Opening DM channel for user: {}", firstId);
                ResponseEntity<JsonNode> openResp = client.post()
                        .uri("/conversations.open")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        // users: "id1,id2,id3" 형식으로 전달
                        .body(Map.of("users", String.join(",", ids)))
                        .retrieve()
                        .toEntity(JsonNode.class);

                JsonNode openBody = openResp.getBody();
                boolean openOk = openResp.getStatusCode().is2xxSuccessful()
                        && openBody != null
                        && openBody.path("ok").asBoolean(false);

                if (!openOk) {
                    log.warn("Failed to open slack conversation. body={}", openBody);
                    return false;
                }

                channelId = openBody.path("channel").path("id").asText();
                if (channelId == null || channelId.isBlank()) {
                    log.warn("Slack channel id not found. body={}", openBody);
                    return false;
                }
            }

            // 2) 메시지 전송
            ResponseEntity<JsonNode> sendResp = client.post()
                    .uri("/chat.postMessage")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "channel", channelId,
                            "text", message
                    ))
                    .retrieve()
                    .toEntity(JsonNode.class);

            JsonNode sendBody = sendResp.getBody();
            boolean sendOk = sendResp.getStatusCode().is2xxSuccessful()
                    && sendBody != null
                    && sendBody.path("ok").asBoolean(false);

            if (!sendOk) {
                log.warn("Failed to send slack message. body={}", sendBody);
            }

            return sendOk;
        } catch (Exception e) {
            log.error("Error while sending slack message", e);
            return false;
        }
    }
}
