package com.healthMini.aiController;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.healthMini.aiService.AdminService;
import com.healthMini.entityDto.HealthCareUser;
import com.healthMini.helper.JWTHelper;
import com.healthMini.helper.JwtUtil;
import com.healthMini.response.ResponseStructure;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/health-care")
public class AdminController {

	@Autowired
	AdminService adminService;

	@Autowired
	private HttpServletRequest request;

	@Autowired
	private JWTHelper jwtHelper;

	@Autowired
	JwtUtil jwtUtil;

	@PostMapping("/admin/create")
	public ResponseEntity<ResponseStructure<String>> createAdmin(@RequestParam String email,
			@RequestParam String password) {
		return adminService.createAdmin(email, password);
	}

	@PostMapping("/admin/login")
	public ResponseEntity<ResponseStructure<String>> adminLogin(@RequestParam String email,
			@RequestParam String password) {
		return adminService.adminLogin(email, password);
	}

	@GetMapping("/users/count")
	public ResponseEntity<ResponseStructure<Long>> getUserCount() {
		ResponseEntity<ResponseStructure<Long>> validationResponse = validateAdminAccess();
		if (validationResponse != null) {
			return validationResponse; // Now correctly returns the expected type
		}
		return adminService.getTotalUserCount();
	}

	@GetMapping("/users/range")
	public ResponseEntity<ResponseStructure<List<HealthCareUser>>> getUsersByRange(@RequestParam int startId,
			@RequestParam int endId, @RequestParam(required = false) String role) {

		ResponseEntity<ResponseStructure<List<HealthCareUser>>> validationResponse = validateAdminAccess();
		if (validationResponse != null) {
			return validationResponse; // Now correctly returns the expected type
		}
		return adminService.getUsersByIdRange(startId, endId, role);
	}

	@GetMapping("/users/search")
	public ResponseEntity<ResponseStructure<HealthCareUser>> getUserByEmailOrPhone(@RequestParam String emph) {

		ResponseEntity<ResponseStructure<HealthCareUser>> validationResponse = validateAdminAccess();
		if (validationResponse != null) {
			return validationResponse; // Now correctly returns the expected type
		}
		return adminService.getUserByEmailOrPhone(emph);
	}

	// METHOD TO CHECK IF THE ADMIN IS PERFORMING THE ACTIONS
	private <T> ResponseEntity<ResponseStructure<T>> validateAdminAccess() {
		String token = request.getHeader("Authorization");

		// Validate JWT token
		ResponseEntity<ResponseStructure<String>> validationResponse = jwtHelper.validateToken(token);
		if (validationResponse != null) {
			return new ResponseEntity<>(convertResponseStructure(validationResponse.getBody()),
					HttpStatus.UNAUTHORIZED);
		}

		// Extract role from token
		String jwtToken = token.substring(7); // Remove "Bearer " prefix
		String role = jwtUtil.extractRole(jwtToken);

		if (!"admin".equalsIgnoreCase(role)) {
			ResponseStructure<T> response = new ResponseStructure<>();
			response.setMessage("Access denied. Only admins can perform this action.");
			response.setCode(HttpStatus.FORBIDDEN.value());
			response.setStatus("error");
			return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
		}

		return null; // If validation passes, return null
	}

	// Method to convert the validation response structure to generic type
	private <T> ResponseStructure<T> convertResponseStructure(ResponseStructure<String> stringResponse) {
		ResponseStructure<T> response = new ResponseStructure<>();
		response.setStatus(stringResponse.getStatus());
		response.setMessage(stringResponse.getMessage());
		response.setCode(stringResponse.getCode());
		response.setPlatform("Admin panel");
		return response;
	}
	
	  @PostMapping("/forgot-password")
	    public ResponseEntity<ResponseStructure<String>> forgotPassword(@RequestParam String emph) {
	        return adminService.forgotPassword(emph);
	    }

	    // Verify OTP (No JWT required)
	    @PostMapping("/verify-otp")
	    public ResponseEntity<ResponseStructure<String>> verifyOtp(@RequestParam String emph, @RequestParam int otp) {
	        return adminService.verifyOtpForPasswordReset(emph, otp);
	    }
	    
	    // RESEND OTP
	    @PostMapping("/resend-otp")
	    public ResponseEntity<ResponseStructure<String>> resendOtp(@RequestParam String emph) {
	        return adminService.resendOtp(emph);
	    }

	
	@PostMapping("/reset-password")
	public ResponseEntity<ResponseStructure<String>> resetPassword(
	        @RequestParam String emph,
	        @RequestParam String newPassword,
	        @RequestParam String confirmPassword) {

	    return adminService.resetPassword(emph, newPassword, confirmPassword);
	}
}
