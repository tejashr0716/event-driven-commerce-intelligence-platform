package com.infosys.commerce.controller;

import com.infosys.commerce.domain.dto.AuthRequest;
import com.infosys.commerce.domain.dto.AuthResponse;
import com.infosys.commerce.domain.model.User;
import com.infosys.commerce.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.authenticate(request));
    }

    @GetMapping("/me")
    public ResponseEntity<User> getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(authService.getByEmail(authentication.getName()));
    }

    @GetMapping("/users")
    public ResponseEntity<List<Map<String, Object>>> getAvailableAccounts() {
        List<Map<String, Object>> users = authService.getAllUsers().stream()
                .map(u -> Map.<String, Object>of(
                        "id", u.getId(),
                        "email", u.getEmail(),
                        "fullName", u.getFullName(),
                        "role", u.getRole()
                ))
                .toList();
        return ResponseEntity.ok(users);
    }
}
