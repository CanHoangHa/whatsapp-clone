package com.hoangha.whatsappclone.ws;

import com.hoangha.whatsappclone.notification.Notification;
import com.hoangha.whatsappclone.notification.NotificationService;
import com.hoangha.whatsappclone.notification.NotificationType;
import com.hoangha.whatsappclone.user.UserPresenceService;
import com.hoangha.whatsappclone.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketEventListener {

    private final UserRepository userRepository;
    private final KafkaTemplate<String, Notification> kafkaTemplate;
    private final UserPresenceService userPresenceService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        log.info("CONNECT EVENT FIRED");

        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) return;

        String userId = user.getName();

        userPresenceService.cancelOffline(userId);

        userRepository.updateLastSeen(userId, LocalDateTime.now());
        Notification notification = Notification.builder()
                .senderId(userId)
                .type(NotificationType.ONLINE)
                .build();
        kafkaTemplate.send("user-status", notification);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        Principal user = accessor.getUser();
        if (user == null) return;

        String userId = user.getName();

        userPresenceService.scheduleOffline(userId, () -> {
            userRepository.updateLastSeen(userId, LocalDateTime.now());

            Notification notification = Notification.builder()
                    .senderId(userId)
                    .type(NotificationType.OFFLINE)
                    .build();
            kafkaTemplate.send("user-status", notification);
        });
    }
}
