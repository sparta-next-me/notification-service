package org.nextme.notificationservice.infrastructure.kafka.consumer;

import org.nextme.notificationservice.application.NotificationService;
import org.nextme.notificationservice.infrastructure.kafka.KafkaConfig;
import org.nextme.notificationservice.infrastructure.kafka.event.MonitoringNotificationEvent;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

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

	/**
	 * monitoring.notification 토픽에서 이벤트를 소비하여 Slack으로 알림 전송
	 */
	@KafkaListener(topics = KafkaConfig.MONITORING_NOTIFICATION_TOPIC, groupId = "notification-service")
	public void handleMonitoringNotification(MonitoringNotificationEvent event) {
		log.info("Received monitoring notification event for {} users", event.slackUserIds().size());

		try {
			boolean success = notificationService.sendToUsers(event.slackUserIds(), event.message());

			if (success) {
				log.info("Successfully sent monitoring notification to {} users", event.slackUserIds().size());
			} else {
				log.warn("Failed to send monitoring notification to some users");
			}

		} catch (Exception e) {
			log.error("Error processing monitoring notification event", e);
			throw e; // Kafka will retry based on configuration
		}
	}
}
