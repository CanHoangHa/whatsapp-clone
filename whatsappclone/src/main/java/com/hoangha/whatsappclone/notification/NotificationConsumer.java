package com.hoangha.whatsappclone.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationConsumer {

    private final NotificationService notificationService;

    // Listener này sẽ lắng nghe topic "message-topic" mà chúng ta đã config
    @KafkaListener(topics = "message-topic", groupId = "whatsapp-clone-group")
    public void consumeNotification(Notification notification) {
        log.info("Consuming notification from Kafka: {}", notification);

        // Sau khi nhận được tin từ Kafka, gọi Service để bắn WebSocket
        notificationService.sendNotification(
                notification.getReceiverId(),
                notification
        );
    }

    @KafkaListener(topics = "user-status", groupId = "whatsapp-clone-group")
    public void consumeUserStatusNotification(Notification notification) {
        log.info("Consuming user status notification from Kafka: {}", notification);

        // SỬA: Gọi hàm broadcast truyền thẳng notification object
        notificationService.broadcastUserStatus(notification);
    }
}