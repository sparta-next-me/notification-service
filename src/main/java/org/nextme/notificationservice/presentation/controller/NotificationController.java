package org.nextme.notificationservice.presentation.controller;

import lombok.RequiredArgsConstructor;
import org.nextme.notificationservice.application.NotificationService;
import org.nextme.notificationservice.presentation.dto.SlackUserMessageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 알림(Notification) REST API
 *
 * - 현재는 Slack 전용 엔드포인트 제공
 * - 다른 서비스(order, payment 등)는 이 엔드포인트를 호출해서 알림 전송
 */
@RestController
@RequestMapping("/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * 여러 유저에게 Slack DM / 그룹 DM 전송
     *
     * POST /v1/notifications/slack/users
     *
     * 예:
     * {
     *   "slackUserIds": ["U09LQLP1YEQ"],
     *   "text": "테스트 메시지입니다."
     * }
     */
    @PostMapping("/slack/users")
    public ResponseEntity<Void> sendToUsers(@RequestBody SlackUserMessageRequest request) {
        boolean ok = notificationService.sendToUsers(
                request.slackUserIds(),
                request.text()
        );

        // 전송 성공/실패에 따라 HTTP 상태 코드 분기
        if (ok) {
            return ResponseEntity.ok().build();
        } else {
            // Slack API/네트워크 문제 등 외부 연동 실패로 간주
            return ResponseEntity.badRequest().build();
        }
    }
}
