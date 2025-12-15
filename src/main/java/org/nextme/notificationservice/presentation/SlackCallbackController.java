package org.nextme.notificationservice.presentation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClient;

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
    private final RestClient.Builder restClientBuilder;

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
                log.info("User approved remediation action: {}, approvedBy: {}", actionValue, userId);

                // promotion-service의 remediation API 호출
                callRemediationAPI(actionValue, userId);

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
     * promotion-service의 remediation API 호출
     */
    private void callRemediationAPI(String actionType, String approvedBy) {
        try {
            log.info("Calling remediation API - actionType: {}, approvedBy: {}", actionType, approvedBy);

            RestClient client = restClientBuilder.baseUrl("http://localhost:11111").build();

            ResponseEntity<JsonNode> response = client.post()
                .uri(uriBuilder -> uriBuilder
                    .path("/v1/promotions/monitoring/remediation/execute")
                    .queryParam("actionType", actionType)
                    .queryParam("approvedBy", approvedBy)
                    .build())
                .retrieve()
                .toEntity(JsonNode.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                JsonNode body = response.getBody();
                if (body != null) {
                    boolean success = body.path("success").asBoolean(false);
                    String message = body.path("message").asText();
                    log.info("Remediation executed - success: {}, message: {}", success, message);
                }
            } else {
                log.warn("Remediation API call failed with status: {}", response.getStatusCode());
            }

        } catch (Exception e) {
            log.error("Failed to call remediation API", e);
        }
    }

}
