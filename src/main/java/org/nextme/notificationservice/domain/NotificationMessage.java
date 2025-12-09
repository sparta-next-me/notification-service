package org.nextme.notificationservice.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.nextme.common.jpa.BaseEntity;
import org.nextme.notificationservice.domain.MessageStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 알림 메시지 이력 엔티티
 *
 * - 누가(receiver_user_id)
 * - 어떤 내용(message)을
 * - 언제(sent_at)
 * - 어떤 결과(status)로 보냈는지 저장
 *
 * 채널(SLACK/EMAIL/SMS 등)은 나중에 필요하면 컬럼 추가
 */
@Getter
@Entity
@Table(name = "p_message")  // 아직 DB 안 만들었으면 여기부터 일반화 추천
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationMessage extends BaseEntity {

    @Id
    @Column(name = "message_id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "receiver_user_id", nullable = false)
    private UUID receiverUserId;

    @Lob // TEXT 매핑
    @Column(name = "message", nullable = false)
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private MessageStatus status;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    // 생성 팩토리 메서드
    public static NotificationMessage create(UUID receiverUserId, String message) {
        NotificationMessage notificationMessage = new NotificationMessage();
        notificationMessage.id = UUID.randomUUID();
        notificationMessage.receiverUserId = receiverUserId;
        notificationMessage.message = message;
        notificationMessage.status = MessageStatus.FAILED; // 처음엔 FAILED or PENDING 등
        return notificationMessage;
    }

    public void markSuccess() {
        this.status = MessageStatus.SUCCESS;
        this.sentAt = LocalDateTime.now();
    }

    public void markFailed() {
        this.status = MessageStatus.FAILED;
        this.sentAt = LocalDateTime.now();
    }
}
