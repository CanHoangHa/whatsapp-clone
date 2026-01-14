package com.hoangha.whatsappclone.chat;

import com.hoangha.whatsappclone.user.User;
import org.springframework.stereotype.Service;

@Service
public class ChatMapper {

    public ChatResponse toChatResponse(Chat chat, String senderId) {
        return ChatResponse.builder()
                .id(chat.getId())
                .name(chat.getConnectedChatName(senderId))
                .unreadCount(chat.getUnreadMessages(senderId))
                .lastMessage(chat.getLastMessage())
                .isRecipientOnline(isRecipientOnline(chat,senderId))
                .senderId(chat.getSender().getId())
                .recipientId(chat.getRecipient().getId())
                .lastMessageTime(chat.getLastMessageTime())
                .build();
    }

    private boolean isRecipientOnline(Chat chat, String senderId) {
        if (chat.getSender().getId().equals(senderId)) {
            return chat.getRecipient().isUserOnline();
        } else {
            return chat.getSender().isUserOnline();
        }
    }
}
