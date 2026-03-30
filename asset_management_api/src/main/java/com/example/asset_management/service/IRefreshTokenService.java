package com.example.asset_management.service;

import com.example.asset_management.model.RefreshToken;
import org.springframework.security.core.userdetails.UserDetails;

public interface IRefreshTokenService {

    String createRefreshToken(UserDetails user);

    RefreshToken verifyExpiration(RefreshToken refreshToken);

    String hashToken(String token);
}
