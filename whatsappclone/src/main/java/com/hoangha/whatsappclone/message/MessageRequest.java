package com.hoangha.whatsappclone.message;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MessageRequest {
    private String content;
    private String senderId;
    private String receiverId;
    private MessageType messageType;
    private String chatId;
}
