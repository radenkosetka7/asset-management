package com.example.asset_management.dto.response;

import java.io.Serializable;

public record AuthResponse(String accessToken, String refreshToken) implements Serializable {
}
