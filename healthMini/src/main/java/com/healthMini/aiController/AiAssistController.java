package com.healthMini.aiController;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthMini.aiService.AiAssistService;
import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.entityDto.HealthData;
import com.healthMini.exceptionHadler.InvalidTokenException;
import com.healthMini.helper.JWTHelper;
import com.healthMini.helper.JwtUtil;
import com.healthMini.repository.HealthCareUserRepository;
import com.healthMini.response.ResponseStructure;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/ai-assist")
public class AiAssistController {

    @Autowired
    private AiAssistService aiAssistService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private JWTHelper jwtHelper;
    
    @Autowired
    HealthCareUserRepository userRepository;
   

    @PostMapping("/health-advice")
    public ResponseEntity<ResponseStructure<String>> getHealthAdvice(@RequestParam String userPrompt) {
        try {
            // Extract and validate JWT token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Remove "Bearer " prefix
            } else {
                throw new InvalidTokenException("Invalid or missing Authorization header");
            }

            // Validate JWT token using helper
            jwtHelper.validateToken(token);

            // If token is valid, proceed with AI assist feature
            return aiAssistService.getHealthAdvice(userPrompt, token);

        } catch (InvalidTokenException e) {
            // Handle invalid token errors
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage(e.getMessage());
            errorResponse.setCode(HttpStatus.UNAUTHORIZED.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
              // Handle unexpected errors
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("An unexpected error occurred: " + e.getMessage());
            errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    
    @PostMapping("/predict") 
    public ResponseEntity<ResponseStructure<String>> predictHealthRisk(@RequestBody HealthData healthData) {
        try {
            // Extract and validate JWT token
            String token = request.getHeader("Authorization");
            if (token != null && token.startsWith("Bearer ")) {
                token = token.substring(7); // Remove "Bearer " prefix
            } else {
                throw new InvalidTokenException("Invalid or missing Authorization header");
            }

            // Validate JWT token
            jwtHelper.validateToken(token);

            // Extract email from token
            int userId = jwtUtil.extractUserId(token);

            Optional<HealthCareUser> optionalUser = userRepository.findById(userId);
            if (optionalUser.isEmpty()) {
                ResponseStructure<String> errorResponse = new ResponseStructure<>();
                errorResponse.setStatus("error");
                errorResponse.setMessage("User not found with id: " + userId);
                errorResponse.setCode(HttpStatus.NOT_FOUND.value());
                return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
            }
            HealthCareUser user = optionalUser.get();



            // Validate input
            if (healthData.getAge() <= 0 || healthData.getHeight() <= 0 || healthData.getWeight() <= 0) {
                throw new IllegalArgumentException("Invalid input data. Age, height, and weight must be positive.");
            }

            // Calculate BMI
           healthData.calculateAndSetBMI();

            // Call the service to get prediction and persist
            String predictionResult = aiAssistService.getPrediction(healthData, user);

            // Response
            ResponseStructure<String> response = new ResponseStructure<>();
            response.setStatus("success");
            response.setMessage("Prediction generated and saved successfully.");
            response.setCode(200);
            response.setData(predictionResult);
            return ResponseEntity.ok(response);

        } catch (InvalidTokenException e) {
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage(e.getMessage());
            errorResponse.setCode(HttpStatus.UNAUTHORIZED.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);

        } catch (IllegalArgumentException e) {
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage(e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);

        } catch (Exception e) {
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("An unexpected error occurred: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

}
