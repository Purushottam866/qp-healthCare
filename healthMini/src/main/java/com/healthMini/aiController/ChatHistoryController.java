package com.healthMini.aiController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthMini.aiService.ChatHistoryService;
import com.healthMini.chatDto.ChatHistoryResponse;
import com.healthMini.chatDto.SessionMessages;
import com.healthMini.exceptionHadler.DataNotFoundException;
import com.healthMini.exceptionHadler.InvalidTokenException;
import com.healthMini.exceptionHadler.UserNotFoundException;
import com.healthMini.helper.JwtUtil;
import com.healthMini.response.ResponseStructure;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/health-care")
public class ChatHistoryController {

    @Autowired
    private ChatHistoryService chatHistoryService;
    
    @Autowired
    private HttpServletRequest request;
    
    @Autowired
    JwtUtil jwtUtil;

 
    @GetMapping("/ai-history")
    public ResponseEntity<ResponseStructure<?>> getChatHistory() {
        try {
        	
        	 // Extract and validate JWT token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Remove "Bearer " prefix
            } else {
                throw new InvalidTokenException("Invalid or missing Authorization header");
            }
            
            // Call service method to fetch chat history
            ResponseStructure<ChatHistoryResponse> response = chatHistoryService.fetchAllChatHistoryByToken(token);
            return ResponseEntity.status(response.getCode()).body(response);
        } catch (DataNotFoundException ex) {
            // Handle DataNotFoundException explicitly
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setMessage(ex.getMessage());
            errorResponse.setStatus("error");
            errorResponse.setCode(404);
            errorResponse.setPlatform("Chat Service");
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);
        } catch (Exception ex) {
            // Handle unexpected errors
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setMessage("An unexpected error occurred: " + ex.getMessage());
            errorResponse.setStatus("error");
            errorResponse.setCode(500);
            errorResponse.setPlatform("Chat Service");
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
    
    @GetMapping("/ai/chat-history")
    public ResponseEntity<?> getChatHistory(@RequestParam int sessionId) {
        try {
            // Extract and validate JWT token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Remove "Bearer " prefix
            } else {
                throw new InvalidTokenException("Invalid or missing Authorization header");
            }

            // Validate token and extract user information
            int userId = jwtUtil.extractUserId(token);
    	    if (userId <= 0) {
    	        throw new UserNotFoundException("User not found for the provided token.");
    	    }

            // Call service method to fetch chat history by session ID
            ResponseStructure<SessionMessages> response = chatHistoryService.fetchChatHistoryBySessionId(sessionId);

            // Return success response
            return ResponseEntity.status(response.getCode()).body(response);

        } catch (InvalidTokenException ex) {
            // Handle invalid or missing JWT token
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setMessage(ex.getMessage());
            errorResponse.setStatus("error");
            errorResponse.setCode(401); // Unauthorized
            errorResponse.setPlatform("Chat Service");
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(errorResponse);

        } catch (DataNotFoundException ex) {
            // Handle case where session or messages are not found
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setMessage(ex.getMessage());
            errorResponse.setStatus("error");
            errorResponse.setCode(404); // Not Found
            errorResponse.setPlatform("Chat Service");
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorResponse);

        } catch (Exception ex) {
            // Handle unexpected errors
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setMessage("An unexpected error occurred: " + ex.getMessage());
            errorResponse.setStatus("error");
            errorResponse.setCode(500); // Internal Server Error
            errorResponse.setPlatform("Chat Service");
            errorResponse.setData(null);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }


}
