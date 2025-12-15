package org.nextme.notificationservice.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Slack Interactive 메시지 Callback 처리 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/slack")
@RequiredArgsConstructor
public class SlackCallbackController {

    private final ObjectMapper objectMapper;

    /**
     * Slack Interactive 메시지 버튼 클릭 Callback
     * Slack은 application/x-www-form-urlencoded 형식으로 payload를 전송
     */
    @PostMapping("/interactive")
    public ResponseEntity<Void> handleInteractive(@RequestParam("payload") String payload) {
        try {
            log.info("Received Slack interactive callback");

            // JSON 파싱
            JsonNode json = objectMapper.readTree(payload);

            // action_id 추출
            String actionId = json.path("actions").get(0).path("action_id").asText();
            String value = json.path("actions").get(0).path("value").asText();
            String userId = json.path("user").path("id").asText();

            log.info("Slack callback - actionId: {}, value: {}, userId: {}", actionId, value, userId);

            // TODO: Kafka 이벤트 발행하여 promotion-service로 전달
            // action_id가 "remediation_approve"이면 실행, "remediation_reject"이면 취소

            if (actionId.endsWith("_approve")) {
                log.info("User approved remediation action: {}", value);
                // Kafka 이벤트 발행
            } else if (actionId.endsWith("_reject")) {
                log.info("User rejected remediation action: {}", value);
            }

            // Slack은 3초 내에 200 OK 응답을 기대
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process Slack interactive callback", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
