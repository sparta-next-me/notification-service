package org.nextme.notificationservice.infrastructure.kafka.event;

import java.util.List;

/**
 * 모니터링 알림 이벤트
 * promotion-service에서 발행한 이벤트를 수신
 */
public record MonitoringNotificationEvent(
	List<String> slackUserIds,
	String message
) {
}
