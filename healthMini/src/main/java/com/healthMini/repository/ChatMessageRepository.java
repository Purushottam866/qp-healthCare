package com.healthMini.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthMini.entityDto.ChatMessage;
import com.healthMini.entityDto.ChatSession;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Integer> {


    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.session s JOIN s.user u WHERE u.userId = :userId AND m.isUserMessage = true AND m.timestamp BETWEEN :startOfDay AND :endOfDay")
    long countUserInputMessagesByUserAndTimestamp(
        @Param("userId") int userId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

	List<ChatMessage> findBySessionOrderByTimestampAsc(ChatSession session);
	
	List<ChatMessage> findBySession_SessionId(int sessionId);

}
