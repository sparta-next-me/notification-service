package org.nextme.notificationservice.infrastructure.kafka.event;

import java.util.List;

/**
 * 모니터링 알림 이벤트
 * promotion-service에서 발행한 이벤트를 수신
 * actionId와 actionValue는 Interactive Button을 위한 필드
 */
public record MonitoringNotificationEvent(
	List<String> slackUserIds,
	String message,
	String actionId,
	String actionValue
) {
	/**
	 * 일반 메시지 생성자 (버튼 없음)
	 */
	public MonitoringNotificationEvent(List<String> slackUserIds, String message) {
		this(slackUserIds, message, null, null);
	}
}
