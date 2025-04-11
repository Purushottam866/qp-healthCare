package com.healthMini.chatDto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
public class ChatHistoryResponse {

	private int userId; // User ID of the person whose chat history is being fetched
	private String username; // Username of the person
	private LocalDate currentDate; // Current date when the response is generated
	private List<SessionMessages> sessions; // List of sessions with their messages
}
