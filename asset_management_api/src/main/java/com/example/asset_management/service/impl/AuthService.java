package com.example.asset_management.service.impl;

import com.example.asset_management.dto.request.SignInRequest;
import com.example.asset_management.dto.response.AuthResponse;
import com.example.asset_management.service.IAuthService;
import com.example.asset_management.service.IRefreshTokenService;
import com.example.asset_management.service.ITokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Transactional
@RequiredArgsConstructor
public class AuthService implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final ITokenService tokenService;
    private final IRefreshTokenService refreshTokenService;

    @Override
    public AuthResponse signIn(SignInRequest request) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(), request.password()
                )
        );
        String accessToken = tokenService.generateAccessToken((UserDetails) auth.getPrincipal());
        String refreshToken = refreshTokenService.createRefreshToken((UserDetails) auth.getPrincipal());
        return new AuthResponse(accessToken, refreshToken);
    }
}
