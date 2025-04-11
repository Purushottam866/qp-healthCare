package com.healthMini.aiController;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthMini.aiService.HealthCareUserService;
import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.exceptionHadler.InvalidTokenException;
import com.healthMini.exceptionHadler.UserNotFoundException;
import com.healthMini.helper.JwtUtil;
import com.healthMini.response.ResponseStructure;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/health-care")
public class HealthCareUserController {

    @Autowired
    HealthCareUserService userService;
    
    @Autowired
    private HttpServletRequest request;

    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/user-signup")
    public ResponseEntity<ResponseStructure<String>> signUpUser(@RequestBody HealthCareUser user) {
        return userService.userSignUp(user);
    }
    
    @GetMapping("/user-verify-otp")
    public ResponseEntity<ResponseStructure<String>> verifyOtp(
            @RequestParam String email,
            @RequestParam int otp) {
        return userService.verifyOtp(email, otp);
    }
    
    @PostMapping("/user-login")
    public ResponseEntity<ResponseStructure<String>> login(@RequestParam String emph, @RequestParam String password) {
        return userService.login(emph, password);
    }
    
    @PostMapping("/user-logout")
    public ResponseEntity<ResponseStructure<String>> logout() {
        ResponseStructure<String> structure = new ResponseStructure<>();
        structure.setMessage("Logged out successfully!");
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }
    
    @DeleteMapping("/delete-user")
    public ResponseEntity<ResponseStructure<String>> deleteUser(
            @RequestParam(required = false) String email) {
        try {
            if (email != null) {
                // Admin access: Delete user by email
                userService.deleteUserByEmail(email);

                ResponseStructure<String> response = new ResponseStructure<>();
                response.setStatus("success");
                response.setMessage("User and all related data deleted successfully by email " + email);
                response.setCode(HttpStatus.OK.value());
                return ResponseEntity.ok(response);
            } else {
                // Authenticated user: Delete user based on JWT token
                String token = request.getHeader("Authorization");
                if (token != null && token.startsWith("Bearer ")) {
                    token = token.substring(7); // Remove "Bearer " prefix
                } else {
                    throw new InvalidTokenException("Invalid or missing Authorization header");
                }

                int userId = jwtUtil.extractUserId(token);
                userService.deleteUserAndRelatedData(userId);

                ResponseStructure<String> response = new ResponseStructure<>();
                response.setStatus("success");
                response.setMessage("User and all related data deleted successfully by JWT token");
                response.setCode(HttpStatus.OK.value());
                return ResponseEntity.ok(response);
            }
        } catch (InvalidTokenException e) {
            // Handle invalid token errors
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage(e.getMessage());
            errorResponse.setCode(HttpStatus.UNAUTHORIZED.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.UNAUTHORIZED);
        } catch (UserNotFoundException e) {
            // Handle case where the user is not found
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage(e.getMessage());
            errorResponse.setCode(HttpStatus.NOT_FOUND.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
        } catch (Exception e) {
            // Handle unexpected errors
            ResponseStructure<String> errorResponse = new ResponseStructure<>();
            errorResponse.setStatus("error");
            errorResponse.setMessage("An unexpected error occurred: " + e.getMessage());
            errorResponse.setCode(HttpStatus.INTERNAL_SERVER_ERROR.value());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}