package com.hoangha.whatsappclone.notification;

import com.hoangha.whatsappclone.message.MessageType;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    private String chatId;
    private String content;
    private String senderId;
    private String receiverId;
    private String chatName;
    private MessageType messageType;
    @Enumerated(EnumType.STRING)
    private NotificationType type;
    private byte[] media;
}
