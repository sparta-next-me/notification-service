package org.nextme.notificationservice.presentation.dto;

import java.util.List;

/**
 * Slack 유저들에게 보낼 메세지 요청 DTO
 */
public record SlackUserMessageRequest(
        List<String> slackUserIds, // Slack user ID 목록
        String text                // 전송할 텍스트
) {}
