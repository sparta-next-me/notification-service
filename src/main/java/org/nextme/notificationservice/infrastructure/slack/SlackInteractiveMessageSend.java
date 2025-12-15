package org.nextme.notificationservice.infrastructure.slack;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Slack Interactive 메시지 (버튼 포함) 전송 구현체
 */
@Slf4j
@Service
@RefreshScope
public class SlackInteractiveMessageSend {

    @Value("${slack.token}")
    private String token;

    private final RestClient client = RestClient.builder()
            .baseUrl("https://slack.com/api")
            .build();

    /**
     * Interactive 메시지 전송 (Yes/No 버튼 포함)
     *
     * @param ids            Slack user ID 목록
     * @param message        메시지 내용
     * @param actionId       버튼 클릭 시 사용할 action_id
     * @param actionValue    버튼 클릭 시 전달할 value
     * @return 전송 성공 여부
     */
    public boolean sendWithButtons(List<String> ids, String message, String actionId, String actionValue) {
        if (ids == null || ids.isEmpty()) {
            log.warn("No slack ids provided");
            return false;
        }

        try {
            // 1) 채널 열기
            ResponseEntity<JsonNode> openResp = client.post()
                    .uri("/conversations.open")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
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

            String channelId = openBody.path("channel").path("id").asText();
            if (channelId == null || channelId.isBlank()) {
                log.warn("Slack channel id not found. body={}", openBody);
                return false;
            }

            // 2) Interactive 메시지 전송 (Block Kit 사용)
            var blocks = buildInteractiveBlocks(message, actionId, actionValue);
            log.info("Built interactive blocks with actionId: {}, actionValue: {}", actionId, actionValue);
            log.debug("Blocks: {}", blocks);

            ResponseEntity<JsonNode> sendResp = client.post()
                    .uri("/chat.postMessage")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "channel", channelId,
                            "text", message,  // fallback text
                            "blocks", blocks
                    ))
                    .retrieve()
                    .toEntity(JsonNode.class);

            JsonNode sendBody = sendResp.getBody();
            boolean sendOk = sendResp.getStatusCode().is2xxSuccessful()
                    && sendBody != null
                    && sendBody.path("ok").asBoolean(false);

            if (!sendOk) {
                log.warn("Failed to send interactive slack message. Status: {}, body={}",
                    sendResp.getStatusCode(), sendBody);
                if (sendBody != null && sendBody.has("error")) {
                    log.error("Slack API error: {}", sendBody.path("error").asText());
                }
            } else {
                log.info("✅ Interactive message sent successfully with buttons");
            }

            return sendOk;
        } catch (Exception e) {
            log.error("Error while sending interactive slack message", e);
            return false;
        }
    }

    /**
     * Slack Block Kit 형식의 Interactive 블록 생성
     */
    private List<Map<String, Object>> buildInteractiveBlocks(String message, String actionId, String actionValue) {
        List<Map<String, Object>> blocks = new java.util.ArrayList<>();

        // 메시지 섹션 (markdown으로 전체 메시지 포함)
        blocks.add(Map.of(
                "type", "section",
                "text", Map.of(
                        "type", "mrkdwn",
                        "text", message
                )
        ));

        // 구분선
        blocks.add(Map.of("type", "divider"));

        // 버튼 액션 섹션
        blocks.add(Map.of(
                "type", "actions",
                "block_id", "actions_block_" + System.currentTimeMillis(),
                "elements", List.of(
                        // ✅ Yes 버튼
                        Map.of(
                                "type", "button",
                                "text", Map.of(
                                        "type", "plain_text",
                                        "text", "✅ 실행",
                                        "emoji", true
                                ),
                                "style", "primary",
                                "action_id", actionId + "_approve",
                                "value", actionValue
                        ),
                        // ❌ No 버튼
                        Map.of(
                                "type", "button",
                                "text", Map.of(
                                        "type", "plain_text",
                                        "text", "❌ 거부",
                                        "emoji", true
                                ),
                                "style", "danger",
                                "action_id", actionId + "_reject",
                                "value", actionValue
                        )
                )
        ));

        return blocks;
    }
}
