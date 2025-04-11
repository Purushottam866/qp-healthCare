package com.healthMini.aiService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.enums.SubscriptionPlan;
import com.healthMini.exceptionHadler.UserNotFoundException;
import com.healthMini.helper.JwtUtil;
import com.healthMini.helper.SecurePassword;
import com.healthMini.helper.SendMail;
import com.healthMini.repository.HealthCareUserRepository;
import com.healthMini.response.ResponseStructure;

import jakarta.transaction.Transactional;

@Service
public class HealthCareUserService {

    @Autowired
    HealthCareUserRepository userRepository;

    @Autowired
    SendMail sendMail;
    
    @Autowired
    JwtUtil jwtUtil;
    
    @Value("${admin.email}")
    private String adminEmail;

    @Value("${admin.password}")
    private String adminPassword;

    @Autowired
    AdminService adminService;
    
    // USER SIGNUP
    public ResponseEntity<ResponseStructure<String>> userSignUp(HealthCareUser user) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        List<HealthCareUser> existingUser = userRepository.findByEmailOrPhoneNumber(user.getEmail(), user.getPhoneNumber());
        if (!existingUser.isEmpty()) {
            structure.setMessage("Account already exists");
            structure.setCode(HttpStatus.NOT_ACCEPTABLE.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_ACCEPTABLE);
        }

        user.setPassword(SecurePassword.encrypt(user.getPassword(), "123"));

        int otp = sendMail.generateOTP();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setCreatedAt(LocalDateTime.now());
        user.setEmailVerified(false);

        // Set default subscription plan to FREE
        user.setSubscriptionPlan(SubscriptionPlan.FREE);

        userRepository.save(user);

        // Send OTP email for verification
        sendMail.sendEmail(user, "Verify Your Email - HealthCare App", "otp_email_template.html");

        structure.setCode(HttpStatus.CREATED.value());
        structure.setStatus("success");
        structure.setMessage("Successfully signed up, please verify your email with OTP.");
        structure.setData("OTP sent to " + user.getEmail());

        return new ResponseEntity<>(structure, HttpStatus.CREATED);
    }


    // EMAIL VERIFICATION
    public ResponseEntity<ResponseStructure<String>> verifyOtp(String email, int otp) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        // Find user by email
        HealthCareUser user = userRepository.findByEmail(email);
        if (user == null) {
            structure.setMessage("User not found");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }

        // Check if OTP is correct
        if (user.getOtp() != otp) {
            structure.setMessage("Invalid OTP");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        // OTP verified, update email verification status
        user.setEmailVerified(true);
        user.setRole("user");
        userRepository.save(user);

        structure.setMessage("Email verified successfully!");
        structure.setCode(HttpStatus.OK.value());
        structure.setData(" The user with email " + user.getEmail() + " can login now");
        structure.setStatus("success");
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }
    
    //User Login
    public ResponseEntity<ResponseStructure<String>> login(String emph, String password) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        // Check if the user is an admin by fetching from DB
        HealthCareUser user = userRepository.findByEmail(emph);
        
        if (user != null && user.getRole().equals("admin")) {
            return adminService.adminLogin(emph, password);
        }

        // Find user by email or phone number
        List<HealthCareUser> users = userRepository.findByEmailOrPhoneNumber(emph, emph);
        if (users.isEmpty()) {
            structure.setMessage("User not found");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }

        user = users.get(0);
        
        // Check if user is verified
       
        if (!user.isEmailVerified()) {
            // Check if OTP has expired
            if (user.isOtpExpired()) {
                // Send new OTP
                int otp = sendMail.generateOTP();
                user.setOtp(otp);
                user.setOtpGeneratedAt(LocalDateTime.now());
                userRepository.save(user);
                
                // Send OTP email for verification
                sendMail.sendEmail(user, "Verify Your Email - HealthCare App", "otp_email_template.html");
                
                structure.setMessage("You have not verified your email. We have sent a new OTP to your email. Please verify first.");
                structure.setCode(HttpStatus.UNAUTHORIZED.value());
                structure.setStatus("error");
                structure.setData("New OTP sent to " + user.getEmail());
                return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
            } else {
                structure.setMessage("You have not verified your email. Please verify first. We have already sent an OTP to your email.");
                structure.setCode(HttpStatus.UNAUTHORIZED.value());
                structure.setStatus("error");
                structure.setData("OTP already sent to " + user.getEmail() + " Please check your Email");
                return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
            }
        }

        // Check password
        if (!SecurePassword.decrypt(user.getPassword(), "123").equals(password)) {
            structure.setMessage("Invalid password");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        user.setRole("user");
        userRepository.save(user);

        // Generate JWT Token with user role
        String token = jwtUtil.generateToken(user.getUserId(), user.getEmail(), user.getRole());

        structure.setMessage("Logged in successfully!");
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        structure.setData("JWT Token: " + token);
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }

    @Transactional
    public void deleteUserByEmail(String email) {
    	HealthCareUser user = userRepository.findByEmail(email);
    	if (user == null) {
    	    throw new UserNotFoundException("User not found with email: " + email);
    	}
        userRepository.delete(user); // Cascades to chat sessions and messages
    }
    
    @Transactional
    public void deleteUserAndRelatedData(int userId) {
        // Find the user by ID or throw an exception if not found
        HealthCareUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));

        // Delete the user (cascades to chat sessions and messages)
        userRepository.delete(user);
    }

}