package org.nextme.notificationservice.infrastructure.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.nextme.notificationservice.application.NotificationService;
import org.nextme.notificationservice.infrastructure.kafka.KafkaConfig;
import org.nextme.notificationservice.infrastructure.slack.SlackInteractiveMessageSend;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 모니터링 알림 이벤트를 소비하는 Kafka Consumer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MonitoringNotificationConsumer {

	private final NotificationService notificationService;
	private final SlackInteractiveMessageSend slackInteractiveMessageSend;
	private final ObjectMapper objectMapper;

	/**
	 * monitoring.notification 토픽에서 이벤트를 소비하여 Slack으로 알림 전송
	 * actionId와 actionValue가 있으면 Interactive Button을 포함한 메시지 전송
	 */
	@KafkaListener(topics = KafkaConfig.MONITORING_NOTIFICATION_TOPIC, groupId = "notification-service")
	public void handleMonitoringNotification(ConsumerRecord<String, String> record) {
		log.warn("===== MonitoringNotificationConsumer 호출됨 =====");
		log.info("Received monitoring notification event from Kafka");
		log.info("Partition: {}, Offset: {}", record.partition(), record.offset());
		String eventJson = record.value();
		log.info("Event JSON Length: {}", eventJson != null ? eventJson.length() : "null");
		log.debug("Event JSON: {}", eventJson);

		try {
			// JSON을 Map으로 파싱
			JsonNode json = objectMapper.readTree(eventJson);

			java.util.List<String> slackUserIds = objectMapper.convertValue(
				json.get("slackUserIds"),
				new com.fasterxml.jackson.core.type.TypeReference<java.util.List<String>>() {}
			);
			String message = json.get("message").asText();
			String actionId = json.get("actionId").isNull() ? null : json.get("actionId").asText();
			String actionValue = json.get("actionValue").isNull() ? null : json.get("actionValue").asText();

			log.info("Parsed event - actionId: {}, actionValue: {}", actionId, actionValue);

			boolean success;

			// actionId가 있으면 Interactive Button 포함 메시지 전송
			if (actionId != null && !actionId.isBlank()) {
				log.info("✅ Sending interactive message with action: {} (value: {})",
					actionId, actionValue);
				success = slackInteractiveMessageSend.sendWithButtons(
					slackUserIds,
					message,
					actionId,
					actionValue
				);
			} else {
				log.info("⚠️ Sending regular message (no actionId provided)");
				// 일반 메시지 전송
				success = notificationService.sendToUsers(slackUserIds, message);
			}

			if (success) {
				log.info("Successfully sent monitoring notification to {} users", slackUserIds.size());
			} else {
				log.warn("Failed to send monitoring notification to some users");
			}

		} catch (Exception e) {
			log.error("Error processing monitoring notification event", e);
			throw new RuntimeException("Failed to process monitoring notification", e);
		}
	}
}
