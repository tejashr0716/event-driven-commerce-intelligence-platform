package com.infosys.commerce.service;

import com.infosys.commerce.config.JwtService;
import com.infosys.commerce.domain.dto.AuthRequest;
import com.infosys.commerce.domain.dto.AuthResponse;
import com.infosys.commerce.domain.enums.UserRole;
import com.infosys.commerce.domain.model.User;
import com.infosys.commerce.domain.repository.UserRepository;
import com.infosys.commerce.exception.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuditService auditService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.auditService = auditService;
    }

    public AuthResponse authenticate(AuthRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);
        auditService.record("USER_LOGIN_SUCCESS", null, user.getEmail(), "Role=" + user.getRole(), null);

        return new AuthResponse(
                token,
                "Bearer",
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole()
        );
    }

    @Transactional(readOnly = true)
    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with email: " + email));
    }

    @Transactional(readOnly = true)
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Transactional
    public void seedDefaultUsers() {
        seedUserIfMissing("customer@infosys.com", "Infosys@2026", "Enterprise Customer", UserRole.CUSTOMER);
        seedUserIfMissing("admin@infosys.com", "Infosys@2026", "Operations Administrator", UserRole.ADMIN);
        seedUserIfMissing("inventory@infosys.com", "Infosys@2026", "Fulfillment & Inventory Lead", UserRole.INVENTORY_MANAGER);
    }

    private void seedUserIfMissing(String email, String rawPassword, String fullName, UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            User user = new User(email, passwordEncoder.encode(rawPassword), fullName, role);
            userRepository.save(user);
            log.info("Seeded user: {} [{}]", email, role);
        }
    }
}
