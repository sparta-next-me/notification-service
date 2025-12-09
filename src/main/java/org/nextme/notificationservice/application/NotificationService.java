package org.nextme.notificationservice.application;

import lombok.RequiredArgsConstructor;
import org.nextme.notificationservice.domain.MessageSend;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 알림 유스케이스
 *
 * - 컨트롤러에서 받은 요청을 도메인 포트(MessageSend)로 넘기는 역할
 * - 현재는 Slack만 사용하지만, 도메인 레벨에서는 채널에 의존하지 않음
 */
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final MessageSend messageSend;

    /**
     * 여러 유저에게 메시지 전송
     *
     * @param slackUserIds Slack user ID 목록
     * @param text         메세지 내용
     * @return 전송 성공 여부
     */
    public boolean sendToUsers(List<String> slackUserIds, String text) {
        return messageSend.send(slackUserIds, text);
    }
}
