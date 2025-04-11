package com.healthMini.aiService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.healthMini.entityDto.ChatMessage;
import com.healthMini.entityDto.ChatSession;
import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.entityDto.HealthData;
import com.healthMini.enums.SubscriptionPlan;
import com.healthMini.exceptionHadler.DailyLimitExceededException;
import com.healthMini.exceptionHadler.UserNotFoundException;
import com.healthMini.helper.JwtUtil;
import com.healthMini.repository.ChatMessageRepository;
import com.healthMini.repository.ChatSessionRepository;
import com.healthMini.repository.HealthCareUserRepository;
import com.healthMini.repository.HealthDataRepository;
import com.healthMini.response.ResponseStructure;

import jakarta.transaction.Transactional;
@Service
public class AiAssistService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Autowired
    private ChatSessionRepository chatSessionRepository;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @Autowired
    private HealthCareUserRepository userRepository;
    
    @Autowired
    HealthDataRepository healthDataRepository;

    @Autowired
    private JwtUtil jwtUtil;

    private RestTemplate restTemplate = new RestTemplate();
    
    @Autowired
    public AiAssistService(RestTemplateBuilder restTemplateBuilder) {
        this.restTemplate = restTemplateBuilder.build();
    }

    public ResponseEntity<ResponseStructure<String>> getHealthAdvice(String userPrompt, String token) {
        try {
            int userId = jwtUtil.extractUserId(token); // Extract user ID from JWT
            HealthCareUser user = validateUser(userId); // Validate user existence

            checkDailyLimit(user); // Check daily limit

            // Retrieve or create a session based on the current day
            ChatSession session = getOrCreateDailySession(user, userPrompt);

            // Fetch past chat history for context
            String chatHistory = getChatHistory(session);

            // Combine chat history with the new user input
            String fullPrompt = chatHistory + "\nUser: " + userPrompt;

            // Save the user's message
            saveMessage(session, userPrompt, true);

            // Generate AI response using chat history
            String aiResponse = generateAIResponse(fullPrompt);

            // Save the AI's response
            saveMessage(session, aiResponse, false);

            // Prepare and return response
            ResponseStructure<String> responseStructure = new ResponseStructure<>();
            responseStructure.setStatus("success");
            responseStructure.setMessage("AI response generated successfully.");
            responseStructure.setData(aiResponse);
            responseStructure.setCode(HttpStatus.OK.value());

            return ResponseEntity.ok(responseStructure);
        } catch (UserNotFoundException | DailyLimitExceededException e) {
            return handleCustomError(e.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            return handleCustomError("An unexpected error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private HealthCareUser validateUser(int userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User with ID " + userId + " not found."));
    }

    private void checkDailyLimit(HealthCareUser user) {
        if (user.getSubscriptionPlan() == SubscriptionPlan.ADMIN) {
            return; // No limit check for admin users
        }

        int dailyLimit = user.getSubscriptionPlan().getDailyLimit();

        long todayCount = chatMessageRepository.countUserInputMessagesByUserAndTimestamp(
                user.getUserId(), getStartOfDay(), getEndOfDay());

        if (todayCount >= dailyLimit) {
            throw new DailyLimitExceededException(
                    "You have reached your daily limit of " + dailyLimit + " user inputs.");
        }
    }

    @Transactional
    private ChatSession getOrCreateDailySession(HealthCareUser user, String firstPrompt) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay(); // 12:00 AM today
        LocalDateTime endOfDay = LocalDate.now().atTime(23, 59, 59); // 11:59 PM today

        // Check if a session exists for today
        List<ChatSession> todaySessions = chatSessionRepository.findByUserUserIdAndCreatedAtBetween(
                user.getUserId(), startOfDay, endOfDay);

        if (!todaySessions.isEmpty()) {
            return todaySessions.get(0); // Return today's session if it exists
        }

        // Create a new session for today if none exists
        ChatSession newSession = new ChatSession();
        newSession.setUser(user);
        newSession.setTitle(generateTitle(firstPrompt)); // Generate title dynamically
        newSession.setCreatedAt(LocalDateTime.now());
        newSession.setExpiresAt(endOfDay); // Session expires at the end of the day

        // Set deletion eligibility to 7 days from now
        newSession.setDeletionEligibleAt(LocalDateTime.now().plusDays(7));

        return chatSessionRepository.save(newSession);
    }


    private String getChatHistory(ChatSession session) {
        List<ChatMessage> messages = chatMessageRepository.findBySessionOrderByTimestampAsc(session);

        StringBuilder chatHistory = new StringBuilder();
        for (ChatMessage message : messages) {
            chatHistory.append(message.isUserMessage() ? "User: " : "AI: ")
                       .append(message.getContent())
                       .append("\n");
        }

        return chatHistory.toString();
    }

    private void saveMessage(ChatSession session, String content, boolean isUserMessage) {
        ChatMessage message = new ChatMessage();
        message.setSession(session);
        message.setContent(content);
        message.setUserMessage(isUserMessage);
        message.setTimestamp(LocalDateTime.now());

        chatMessageRepository.save(message);
    }

    private String generateAIResponse(String userPrompt) {
        try {
            String escapedPrompt = userPrompt.replace("\"", "\\\"");
            
            String customizedPrompt = "You are a knowledgeable and empathetic AI health assistant.\\n\\n" 
                    + "First, analyze if the user's concern is related to health or healthcare.\\n" 
                    + "If it is NOT health-related, respond ONLY with: \\\"Sorry, please share a healthcare concern and I'll be happy to help.\\\"\\n\\n"
                    + "If it IS health-related, provide helpful insights formatted as follows:\\n\\n"
                    + "1. **Health Overview**: Briefly explain the condition.\\n"
                    + "2. **Diagnosis Methods**: Common ways to diagnose this issue.\\n"
                    + "3. **Treatment Options**: Possible treatments or solutions.\\n"
                    + "4. **Recovery & Cure**: How to manage and potentially cure it.\\n"
                    + "5. **Do's and Don'ts**: Key lifestyle changes or precautions.\\n\\n"
                    + "Keep responses clear, practical, and easy to understand.\\n\\n"
                    + "User's concern: " + escapedPrompt;

            String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + customizedPrompt + "\" } ] } ] }";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");

            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(apiUrl + "?key=" + apiKey, HttpMethod.POST, entity,
                    String.class);

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode textNode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text");

            return textNode.asText();
        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to generate AI response: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }

    private String generateTitle(String prompt) {
        String datePart = "Daily Chat: " + LocalDate.now(); // Add today's date
        String promptPart = prompt.length() > 30 ? prompt.substring(0, 30) + "..." : prompt; // Snippet of prompt
        return promptPart + " - " + datePart; // Combine date and prompt snippet
    }

    private ResponseEntity<ResponseStructure<String>> handleCustomError(String errorMessage, HttpStatus status) {
        ResponseStructure<String> responseStructure = new ResponseStructure<>();
        responseStructure.setStatus("error");
        responseStructure.setMessage(errorMessage);
        responseStructure.setData(null);
        responseStructure.setCode(status.value());

        return ResponseEntity.status(status).body(responseStructure);
    }

    private LocalDateTime getStartOfDay() {
        return LocalDate.now().atStartOfDay();
    }

    private LocalDateTime getEndOfDay() {
        return LocalDate.now().atTime(23, 59, 59);
    }
    
    
    
    // PREDECTION HEALTH DATA
    public String getPrediction(HealthData healthData, HealthCareUser user) {
        try {
            // Restrict FREE plan to 1 input per 7 days
            if (user.getSubscriptionPlan() == SubscriptionPlan.FREE) {
                LocalDateTime lastPrompt = user.getLastPromptTime();
                if (lastPrompt != null && lastPrompt.plusDays(7).isAfter(LocalDateTime.now())) {
                    throw new RuntimeException("As a FREE user, you can submit only 1 health analysis per 7 days.");
                }
            }

            healthData.setUser(user);

            // Prompt preparation
            String escapedPrompt = preparePrompt(healthData).replace("\"", "\\\"");
            String customizedPrompt = "You are a knowledgeable and empathetic AI health assistant.\n\n"
                    + "Analyze the user's health data and provide insights in the following format:\n\n"
                    + "1. **Health Overview**: Summarize the user's health condition.\n"
                    + "2. **Risk Assessment**: Assess potential health risks.\n"
                    + "3. **Recommendations**: Suggest lifestyle changes or treatments.\n"
                    + "4. **Precautions**: Highlight key precautions to take.\n\n"
                    + "User's health data: " + escapedPrompt;

            String requestBody = "{ \"contents\": [ { \"parts\": [ { \"text\": \"" + customizedPrompt + "\" } ] } ] }";

            HttpHeaders headers = new HttpHeaders();
            headers.set("Content-Type", "application/json");
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            String fullApiUrl = apiUrl + "?key=" + apiKey;

            ResponseEntity<String> response = restTemplate.exchange(fullApiUrl, HttpMethod.POST, entity, String.class);
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode textNode = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text");

            // Save prediction result
            String prediction = textNode.asText();
            healthData.setPredictionResult(prediction);
            healthDataRepository.save(healthData);
 
            // Update last prompt time for FREE users
            if (user.getSubscriptionPlan() == SubscriptionPlan.FREE) {
                user.setLastPromptTime(LocalDateTime.now());
                userRepository.save(user); // Assuming you autowired this repository
            } 

            return prediction;

        } catch (HttpClientErrorException e) {
            throw new RuntimeException("Failed to generate prediction: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            throw new RuntimeException("Unexpected error: " + e.getMessage());
        }
    }

    private String preparePrompt(HealthData healthData) {
        return String.format(
                "{ Age: %d, Height: %.2f cm, Weight: %.2f kg, Exercise Frequency: %s, Family History: %s, Diet: %s, Sugary Drink Consumption: %s, High Blood Pressure: %s, Stress Level: %s }",
                healthData.getAge(),
                healthData.getHeight(),
                healthData.getWeight(),
                healthData.getExerciseFrequency(),
                healthData.isFamilyHistory() ? "Yes" : "No",
                healthData.getDiet(),
                healthData.getSugaryDrinkConsumption(),
                healthData.isHighBloodPressure() ? "Yes" : "No",
                healthData.getStressLevel()
        );
    }	

}

