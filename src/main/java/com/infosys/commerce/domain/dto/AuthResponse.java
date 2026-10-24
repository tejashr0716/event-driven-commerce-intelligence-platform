package com.infosys.commerce.domain.dto;

import com.infosys.commerce.domain.enums.UserRole;

public record AuthResponse(
    String token,
    String tokenType,
    Long userId,
    String email,
    String fullName,
    UserRole role
) {}
