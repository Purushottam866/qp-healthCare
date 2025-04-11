package com.healthMini.aiService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.enums.SubscriptionPlan;
import com.healthMini.helper.JwtUtil;
import com.healthMini.helper.SecurePassword;
import com.healthMini.helper.SendMail;
import com.healthMini.repository.HealthCareUserRepository;
import com.healthMini.response.ResponseStructure;

@Service
public class AdminService {

    @Autowired
    JwtUtil jwtUtil;
    
    @Autowired
    HealthCareUserRepository userRepository;
    
    @Autowired
    SendMail sendMail;
	
    //Admin Creation
    public ResponseEntity<ResponseStructure<String>> createAdmin(String email, String password) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        // Check if admin already exists
        if (userRepository.findByEmail(email) != null) {
            structure.setMessage("Admin with this email already exists");
            structure.setCode(HttpStatus.CONFLICT.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.CONFLICT);
        }

        HealthCareUser admin = new HealthCareUser();
        admin.setEmail(email);
        admin.setPassword(SecurePassword.encrypt(password, "123")); // Encrypt password
        admin.setRole("admin"); // Set role as "admin"
        admin.setSubscriptionPlan(SubscriptionPlan.ADMIN); // Set subscription plan to ADMIN
        admin.setEmailVerified(true);
        admin.setCreatedAt(LocalDateTime.now());

        userRepository.save(admin);

        structure.setMessage("Admin account created successfully!");
        structure.setCode(HttpStatus.CREATED.value());
        structure.setStatus("success");
        structure.setData("The email " + email + " has been granted admin access.");
        return new ResponseEntity<>(structure, HttpStatus.CREATED);
    }

    
    public ResponseEntity<ResponseStructure<String>> adminLogin(String email, String password) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        // Fetch admin user by email
        HealthCareUser admin = userRepository.findByEmail(email);

        // Check if admin exists and has "admin" role
        if (admin == null || !admin.getRole().equals("admin")) {
            structure.setMessage("Admin account not found");
            structure.setCode(HttpStatus.UNAUTHORIZED.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
        }

        // Verify password
        if (!SecurePassword.decrypt(admin.getPassword(), "123").equals(password)) {
            structure.setMessage("Invalid password");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        // Generate JWT Token with admin role
        String token = jwtUtil.generateToken(admin.getUserId(), email, "admin");

        structure.setMessage("Admin logged in successfully!");
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        structure.setData("JWT Token: " + token);
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }

    //TOTAL USERS COUNT
    public ResponseEntity<ResponseStructure<Long>> getTotalUserCount() {
        long count = userRepository.count();

        ResponseStructure<Long> response = new ResponseStructure<>();
        response.setStatus("success");
        response.setMessage("Total users count fetched successfully");
        response.setData(count);
        response.setCode(200);
        response.setPlatform("Admin Panel");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // FETCH USERS BETWEEN USER ID RANGE 
    public ResponseEntity<ResponseStructure<List<HealthCareUser>>> getUsersByIdRange(int startId, int endId, String role) {
        List<HealthCareUser> users;
        
        if (role == null || role.isEmpty()) {
            users = userRepository.findByUserIdBetween(startId, endId, Sort.by(Sort.Direction.ASC, "userId"));
        } else {
            users = userRepository.findByUserIdBetweenAndRole(startId, endId, role, Sort.by(Sort.Direction.ASC, "userId"));
        }

        if (users.isEmpty()) {
            return generateErrorResponse("No users found in the given range.");
        }

        ResponseStructure<List<HealthCareUser>> response = new ResponseStructure<>();
        response.setStatus("success");
        response.setMessage("Users fetched successfully");
        response.setData(users);
        response.setCode(200);
        response.setPlatform("Admin Panel");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    // FETCH USERS BY EMAIL OR PHONE
    public ResponseEntity<ResponseStructure<HealthCareUser>> getUserByEmailOrPhone(String query) {
        List<HealthCareUser> users = userRepository.findByEmailOrPhoneNumber(query, query);

        if (users.isEmpty()) {
            return generateErrorResponse("No user found with the given email or phone.");
        }

        ResponseStructure<HealthCareUser> response = new ResponseStructure<>();
        response.setStatus("success");
        response.setMessage("User found successfully");
        response.setData(users.get(0));
        response.setCode(200);
        response.setPlatform("Admin Panel");

        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    private <T> ResponseEntity<ResponseStructure<T>> generateErrorResponse(String message) {
        ResponseStructure<T> response = new ResponseStructure<>();
        response.setStatus("error");
        response.setMessage(message);
        response.setData(null);
        response.setCode(404);
        response.setPlatform("Admin Panel");

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    // FORGOT PASSWORD
    public ResponseEntity<ResponseStructure<String>> forgotPassword(String emph) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        List<HealthCareUser> users = userRepository.findByEmailOrPhoneNumber(emph, emph);
        if (users.isEmpty()) {
            structure.setMessage("User not found");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }

        HealthCareUser user = users.get(0);

        // Generate OTP
        int otp = sendMail.generateOTP();
        user.setOtp(otp);
        user.setOtpGeneratedAt(LocalDateTime.now());
        user.setEmailVerified(false); 
        userRepository.save(user);

        // Send Password Reset OTP Email
        sendMail.sendEmail(user, "Your OTP for Password Reset", "password_reset_template.html");

        structure.setMessage("OTP sent successfully.");
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        structure.setData("OTP sent to " + user.getEmail());
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }

    
    // OTP VERIFICATION FOR PORGOT PASSWORD
    public ResponseEntity<ResponseStructure<String>> verifyOtpForPasswordReset(String emph, int otp) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        List<HealthCareUser> users = userRepository.findByEmailOrPhoneNumber(emph, emph);
        if (users.isEmpty()) {
            structure.setMessage("User not found");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }

        HealthCareUser user = users.get(0);

        if (user.getOtp() != otp) {
            structure.setMessage("Invalid OTP");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        if (user.isOtpExpired()) {
            structure.setMessage("OTP expired. Request a new one.");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        // Mark OTP as verified
        user.setEmailVerified(true);
        userRepository.save(user);

        structure.setMessage("OTP verified. You can now reset your password.");
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }
    
    // RESEND OTP IF EXPIRED
    public ResponseEntity<ResponseStructure<String>> resendOtp(String emph) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        List<HealthCareUser> users = userRepository.findByEmailOrPhoneNumber(emph, emph);
        if (users.isEmpty()) {
            structure.setMessage("User not found");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }

        HealthCareUser user = users.get(0);

        // Check if OTP is still valid (within 30 minutes)
        if (user.getOtpGeneratedAt() != null && user.getOtpGeneratedAt().plusMinutes(30).isAfter(LocalDateTime.now())) {
            structure.setMessage("OTP is still valid. Please check your email.");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        // Generate a new OTP
        int newOtp = sendMail.generateOTP();
        user.setOtp(newOtp);
        user.setOtpGeneratedAt(LocalDateTime.now());

        // Save updated user data
        userRepository.save(user);

        // Send OTP to email
        sendMail.sendEmail(user, "Your OTP for Password Reset", "password_reset_template.html");

        structure.setMessage("A new OTP has been sent to " + user.getEmail());
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }


 // RESET PASSWORD AFTER OTP VERIFICATION (WITH CONFIRM PASSWORD)
    public ResponseEntity<ResponseStructure<String>> resetPassword(String emph, String newPassword, String confirmPassword) {
        ResponseStructure<String> structure = new ResponseStructure<>();

        List<HealthCareUser> users = userRepository.findByEmailOrPhoneNumber(emph, emph);
        if (users.isEmpty()) {
            structure.setMessage("User not found");
            structure.setCode(HttpStatus.NOT_FOUND.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.NOT_FOUND);
        }

        HealthCareUser user = users.get(0);

        // Ensure OTP was verified before allowing password reset
        if (!user.isEmailVerified()) {
            structure.setMessage("OTP verification required before resetting password.");
            structure.setCode(HttpStatus.UNAUTHORIZED.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
        }

        // Validate password confirmation
        if (!newPassword.equals(confirmPassword)) {
            structure.setMessage("New password and confirm password do not match.");
            structure.setCode(HttpStatus.BAD_REQUEST.value());
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.BAD_REQUEST);
        }

        // Encrypt and update password
        String encryptedPassword = SecurePassword.encrypt(newPassword, "123");
        user.setPassword(encryptedPassword);

        // Save the updated user details (keeping emailVerified status unchanged)
        userRepository.save(user);

        structure.setMessage("Password reset successfully. You can now log in.");
        structure.setCode(HttpStatus.OK.value());
        structure.setStatus("success");
        return new ResponseEntity<>(structure, HttpStatus.OK);
    }


    
}
