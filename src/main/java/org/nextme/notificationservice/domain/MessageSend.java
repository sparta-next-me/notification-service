package org.nextme.notificationservice.domain;

import java.util.List;

/**
 * 알림 전송 도메인 포트
 *
 * - 알림을 "어디로 / 어떻게" 보내는지는 구현체(infrastructure)가 담당
 * - 도메인/애플리케이션 레이어에서는 이 인터페이스만 의존
 */
public interface MessageSend {

    /**
     * 여러 Slack 유저에게 메시지 전송
     *
     * @param ids    Slack user ID
     * @param message 전송할 텍스트 메세지
     * @return 전송 성공 여부
     */
    boolean send(List<String> ids, String message);
}
