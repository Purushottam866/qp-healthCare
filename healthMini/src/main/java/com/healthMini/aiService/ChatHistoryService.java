package com.healthMini.aiService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.healthMini.chatDto.ChatHistoryResponse;
import com.healthMini.chatDto.MessageDTO;
import com.healthMini.chatDto.SessionMessages;
import com.healthMini.entityDto.ChatMessage;
import com.healthMini.entityDto.ChatSession;
import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.exceptionHadler.DataNotFoundException;
import com.healthMini.exceptionHadler.UserNotFoundException;
import com.healthMini.helper.JwtUtil;
import com.healthMini.repository.ChatMessageRepository;
import com.healthMini.repository.ChatSessionRepository;
import com.healthMini.repository.HealthCareUserRepository;
import com.healthMini.response.ResponseStructure;

@Service
public class ChatHistoryService {

    @Autowired
    private ChatSessionRepository chatSessionRepo;

    @Autowired
    private ChatMessageRepository chatMessageRepo;
    
    @Autowired
    HealthCareUserRepository userRepository;
    
	@Autowired
	private JwtUtil jwtUtil;
	
	@Autowired
	ChatSessionRepository chatSessionRepository;

	// FETCH ALL CHAT BY OF A USER
	public ResponseStructure<ChatHistoryResponse> fetchAllChatHistoryByToken(String token) {
	    ResponseStructure<ChatHistoryResponse> response = new ResponseStructure<>();

	    // Extract user ID from token
	    int userId = jwtUtil.extractUserId(token);
	    if (userId <= 0) {
	        throw new UserNotFoundException("User not found for the provided token.");
	    }

	    // Fetch user details (e.g., username)
	    HealthCareUser user = userRepository.findById(userId)
	            .orElseThrow(() -> new UserNotFoundException("User not found for ID: " + userId));

	    // Fetch all sessions for this user up to the current date
	    List<ChatSession> sessions = chatSessionRepo.findByUser_UserIdAndCreatedAtBefore(userId, LocalDateTime.now());
	    if (sessions.isEmpty()) {
	        throw new DataNotFoundException("No chat sessions found for the user.");
	    }

	    // Fetch messages for each session
	    List<SessionMessages> sessionMessages = sessions.stream()
	            .map(session -> {
	                List<ChatMessage> messages = chatMessageRepo.findBySession_SessionId(session.getSessionId());
	                return mapToSessionMessages(session, messages);
	            })
	            .collect(Collectors.toList());

	    // Build response structure
	    ChatHistoryResponse chatHistoryResponse = new ChatHistoryResponse();
	    chatHistoryResponse.setUserId(user.getUserId());
	    chatHistoryResponse.setUsername(user.getFullName());
	    chatHistoryResponse.setSessions(sessionMessages);

	    response.setMessage("Chat history fetched successfully.");
	    response.setStatus("success");
	    response.setCode(200);
	    response.setPlatform("Chat Service");
	    response.setData(chatHistoryResponse);

	    return response;
	}


	private SessionMessages mapToSessionMessages(ChatSession session, List<ChatMessage> messages) {
	    SessionMessages sessionMsg = new SessionMessages();
	    sessionMsg.setSessionId(session.getSessionId());
	    sessionMsg.setTitle(session.getTitle());
	    sessionMsg.setMessages(messages.stream()
	            .map(msg -> new MessageDTO(msg.getContent(), msg.isUserMessage()))
	            .collect(Collectors.toList()));
	    return sessionMsg;
	}

	// FETCH CHAT BY SESSION ID
	public ResponseStructure<SessionMessages> fetchChatHistoryBySessionId(int sessionId) {
	    ResponseStructure<SessionMessages> response = new ResponseStructure<>();

	    // Fetch session details
	    ChatSession session = chatSessionRepo.findById(sessionId)
	            .orElseThrow(() -> new DataNotFoundException("No session found for ID: " + sessionId));

	    // Fetch messages for this session
	    List<ChatMessage> messages = chatMessageRepo.findBySession_SessionId(session.getSessionId());
	    if (messages.isEmpty()) {
	        throw new DataNotFoundException("No messages found for session ID: " + session.getSessionId());
	    }

	    // Map session and messages to response structure
	    SessionMessages sessionMessages = mapToSessionMessages(session, messages);

	    // Build response
	    response.setMessage("Chat history fetched successfully.");
	    response.setStatus("success");
	    response.setCode(200);
	    response.setPlatform("Chat Service");
	    response.setData(sessionMessages);

	    return response;
	}

	
	 @Scheduled(cron = "0 0 0 * * ?") // Every day at midnight
	    public ResponseStructure<String> deleteOldSessions() {
	        LocalDateTime now = LocalDateTime.now();

	        try {
	            // Find sessions eligible for deletion
	            List<ChatSession> oldSessions = chatSessionRepository.findByDeletionEligibleAtBefore(now);

	            if (!oldSessions.isEmpty()) {
	                // Delete all eligible sessions
	                chatSessionRepository.deleteAll(oldSessions);

	                // Prepare success response
	                ResponseStructure<String> response = new ResponseStructure<>();
	                response.setStatus("success");
	                response.setMessage("Deleted " + oldSessions.size() + " old sessions.");
	                response.setCode(200);
	                response.setData("Deletion completed at: " + now);

	                // Print response for debugging
	                System.out.println(response);

	                return response;
	            } else {
	                // Prepare success response when no sessions are eligible
	                ResponseStructure<String> response = new ResponseStructure<>();
	                response.setStatus("success");
	                response.setMessage("No sessions eligible for deletion.");
	                response.setCode(200);
	                response.setData("Checked at: " + now);

	                // Print response for debugging
	                System.out.println(response);

	                return response;
	            }
	        } catch (Exception e) {
	            // Prepare error response in case of failure
	            ResponseStructure<String> errorResponse = new ResponseStructure<>();
	            errorResponse.setStatus("error");
	            errorResponse.setMessage("Failed to delete old sessions. Error: " + e.getMessage());
	            errorResponse.setCode(500);
	            errorResponse.setData("Error occurred at: " + now);

	            // Print error response for debugging
	            System.out.println(errorResponse);

	            return errorResponse;
	        }
	    }
}


