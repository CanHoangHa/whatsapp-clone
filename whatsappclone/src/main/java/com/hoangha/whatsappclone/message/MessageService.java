package com.hoangha.whatsappclone.message;

import com.hoangha.whatsappclone.chat.Chat;
import com.hoangha.whatsappclone.chat.ChatRepository;
import com.hoangha.whatsappclone.file.FileService;
import com.hoangha.whatsappclone.file.FileUtils;
import com.hoangha.whatsappclone.notification.Notification;
import com.hoangha.whatsappclone.notification.NotificationService;
import com.hoangha.whatsappclone.notification.NotificationType;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final MessageRepository messageRepository;
    private final ChatRepository chatRepository;
    private final MessageMapper mapper;
    private final FileService fileService;
    private final NotificationService notificationService;

    public void saveMessage(MessageRequest messageRequest){
        Chat chat = chatRepository.findById(messageRequest.getChatId())
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        Message message = new Message();
        message.setChat(chat);
        message.setSenderId(messageRequest.getSenderId());
        message.setContent(messageRequest.getContent());
        message.setReceiverId(messageRequest.getReceiverId());
        message.setType(messageRequest.getMessageType());
        message.setState(MessageState.SENT);

        messageRepository.save(message);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .senderId(messageRequest.getSenderId())
                .receiverId(messageRequest.getReceiverId())
                .content(messageRequest.getContent())
                .messageType(messageRequest.getMessageType())
                .type(NotificationType.MESSAGE)
                .chatName(chat.getChatName(message.getSenderId()))
                .build();

        notificationService.sendNotification(message.getReceiverId(), notification);
    }

    public List<MessageResponse> findChatMessages(String chatId) {
        return messageRepository.findMessagesByChatId(chatId)
                .stream()
                .map(mapper::toMessageResponse)
                .toList();
    }

    @Transactional
    public void setMessagesToSeen(String chatId, Authentication authentication) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        final String recipientId = getRecipientId(chat, authentication);
        final String senderId = getSenderId(chat, authentication);

        messageRepository.setMessagesToSeenByChatId(chatId, MessageState.SEEN);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .senderId(senderId)
                .receiverId(recipientId)
                .type(NotificationType.SEEN)
                .build();

        notificationService.sendNotification(recipientId, notification);
    }



    public void uploadMediaMessage(String chatId, MultipartFile file, Authentication authentication) {
        Chat chat = chatRepository.findById(chatId)
                .orElseThrow(() -> new EntityNotFoundException("Chat not found"));

        final String senderId = getSenderId(chat, authentication);
        final String receiverId = getRecipientId(chat, authentication);

        final String filePath = fileService.saveFile(file, senderId);

        Message message = new Message();
        message.setChat(chat);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setType(MessageType.IMAGE);
        message.setState(MessageState.SENT);
        message.setMediaFilePath(filePath);
        messageRepository.save(message);

        Notification notification = Notification.builder()
                .chatId(chat.getId())
                .senderId(senderId)
                .receiverId(receiverId)
                .messageType(MessageType.IMAGE)
                .type(NotificationType.IMAGE)
                .media(FileUtils.readFileFromLocation(filePath))
                .build();

        notificationService.sendNotification(message.getReceiverId(), notification);
    }

    private String getSenderId(Chat chat, Authentication authentication) {
        if(chat.getSender().getId().equals(authentication.getName())) {
            return chat.getSender().getId();
        }
        return chat.getRecipient().getId();
    }

    private String getRecipientId(Chat chat, Authentication authentication) {
        if(chat.getSender().getId().equals(authentication.getName())) {
            return chat.getRecipient().getId();
        }
        return chat.getSender().getId();
    }
}
