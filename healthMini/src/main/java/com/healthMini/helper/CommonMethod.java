package com.healthMini.helper;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.healthMini.response.ResponseStructure;

@Service
public class CommonMethod {

    @Autowired
    private JwtUtil jwtUtil;

    public Object validateToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            ResponseStructure<String> structure = new ResponseStructure<>();
            structure.setCode(115);
            structure.setMessage("Missing or invalid authorization token");
            structure.setStatus("error");
            return new ResponseEntity<>(structure, HttpStatus.UNAUTHORIZED);
        }
        String jwtToken = token.substring(7);
        int userId = jwtUtil.extractUserId(jwtToken);
        return userId;
    }

    public boolean isAdmin(String token) {
        String role = jwtUtil.extractRole(token.substring(7));
        return "admin".equals(role);
    }
}

