package com.example.asset_management.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "RefreshToken is required")
        String refreshToken) {
}
