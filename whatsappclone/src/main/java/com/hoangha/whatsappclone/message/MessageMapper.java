package com.hoangha.whatsappclone.message;

import org.springframework.stereotype.Service;

@Service
public class MessageMapper {

    public MessageResponse toMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .receiverId(message.getReceiverId())
                .content(message.getContent())
                .type(message.getType())
                .state(message.getState())
                .createdAt(message.getCreatedDate())
                // todo  read teh media file
                .build();
    }
}
