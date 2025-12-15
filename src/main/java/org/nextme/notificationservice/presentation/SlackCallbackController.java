package org.nextme.notificationservice.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final KafkaTemplate<String, RemediationActionEvent> kafkaTemplate;

    // Kafka topic 상수
    private static final String REMEDIATION_TOPIC = "monitoring.remediation";

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
            String actionValue = json.path("actions").get(0).path("value").asText();
            String userId = json.path("user").path("id").asText();

            log.info("Slack callback - actionId: {}, actionValue: {}, userId: {}", actionId, actionValue, userId);

            // actionId가 "monitoring_action_approve"이면 실행
            if (actionId.endsWith("_approve")) {
                log.info("User approved remediation action: {}", actionValue);

                // Kafka 이벤트 발행하여 promotion-service의 RemediationExecutor 실행
                RemediationActionEvent event = new RemediationActionEvent(actionValue, userId);
                kafkaTemplate.send(REMEDIATION_TOPIC, "remediation", event);

                log.info("Remediation event published for action: {}", actionValue);
            } else if (actionId.endsWith("_reject")) {
                log.info("User rejected remediation action: {}", actionValue);
            }

            // Slack은 3초 내에 200 OK 응답을 기대
            return ResponseEntity.ok().build();

        } catch (Exception e) {
            log.error("Failed to process Slack interactive callback", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Remediation Action 이벤트
     */
    public static record RemediationActionEvent(
        String actionType,
        String approvedBy
    ) {}
}
