package org.nextme.notificationservice.infrastructure.kafka;

import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfig {

	// Topic 이름
	public static final String MONITORING_NOTIFICATION_TOPIC = "monitoring.notification";
}
