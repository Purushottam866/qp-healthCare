package com.healthMini.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.healthMini.response.ResponseStructure;

@Service
public class JWTHelper {

	 @Autowired
	    private JwtUtil jwtUtil;

	    public ResponseEntity<ResponseStructure<String>> validateToken(String token) {
	        if (token == null || !token.startsWith("Bearer ")) {
	            ResponseStructure<String> structure = new ResponseStructure<>();
	            structure.setStatus("error");
	            structure.setMessage("Missing or invalid authorization token");
	            structure.setCode(HttpStatus.UNAUTHORIZED.value());
	            structure.setPlatform("AI Assistant");
	            return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
	        }

	        String jwtToken = token.substring(7); // Remove "Bearer " prefix
	        try {
	            int userId = jwtUtil.extractUserId(jwtToken); // Extract user ID from the token
	            return null; // Token is valid, return null to indicate success
	        } catch (Exception e) {
	            ResponseStructure<String> structure = new ResponseStructure<>();
	            structure.setStatus("error");
	            structure.setMessage("Invalid JWT token: " + e.getMessage());
	            structure.setCode(HttpStatus.UNAUTHORIZED.value());
	            structure.setPlatform("AI Assistant");
	            return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
	        }
	    }
}
