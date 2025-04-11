package com.healthMini.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.healthMini.entityDto.ChatSession;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Integer> {

    List<ChatSession> findByUserUserIdAndExpiresAtAfter(int userId, LocalDateTime now);

    @Query("SELECT COUNT(m) FROM ChatMessage m JOIN m.session s WHERE s.user.userId = :userId AND m.timestamp BETWEEN :startOfDay AND :endOfDay")
    long countMessagesByUserAndTimestamp(
        @Param("userId") int userId,
        @Param("startOfDay") LocalDateTime startOfDay,
        @Param("endOfDay") LocalDateTime endOfDay
    );

    @Modifying
    @Query("DELETE FROM ChatSession cs WHERE cs.expiresAt <= :now")
    void deleteExpiredSessions(LocalDateTime now);

    List<ChatSession> findByUser_UserId(int userId);

	List<ChatSession> findByUserUserIdAndCreatedAtBetween(int userId, LocalDateTime startOfDay, LocalDateTime endOfDay);

	List<ChatSession> findByDeletionEligibleAtBefore(LocalDateTime now);

	List<ChatSession> findByUser_UserIdAndCreatedAtBefore(int userId, LocalDateTime now);
}