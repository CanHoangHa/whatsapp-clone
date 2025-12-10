package com.hoangha.whatsappclone.message;


import com.hoangha.whatsappclone.chat.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {
    @Query(name = MessageConstants.FIND_MESSAGES_BY_CHAT_ID)
    List<Message> findMessagesByChatId(@Param("chatId") String chatId);

    @Modifying
    @Query(name = MessageConstants.SET_MESSAGES_TO_SEEN_BY_CHAT)
    int setMessagesToSeenByChatId(@Param("chatId") String chatId, // Đổi void -> int
                                  @Param("newState") MessageState newState,
                                  @Param("currentUserId") String currentUserId);
}
