package com.hoangha.whatsappclone.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;

    public void sendNotification(String userId, Notification notification) {
        log.info("Sending WS notification to userId {} with payload {}", userId, notification);
        messagingTemplate.convertAndSendToUser(
                userId,
                "/chat",
                notification
        );
    }

    public void broadcastUserStatus(Notification notification){
        log.info("Sending WS user status with payload {}", notification);

        // Bắn thẳng object nhận từ Kafka xuống Client
        messagingTemplate.convertAndSend(
                "/topic/user-status",
                notification
        );
    }

}
