package com.example.asset_management.controller;

import com.example.asset_management.dto.request.RefreshTokenRequest;
import com.example.asset_management.dto.request.SignInRequest;
import com.example.asset_management.dto.response.AuthResponse;
import com.example.asset_management.exception.HttpException;
import com.example.asset_management.model.CustomUserDetails;
import com.example.asset_management.model.RefreshToken;
import com.example.asset_management.repository.RefreshTokenRepository;
import com.example.asset_management.service.IAuthService;
import com.example.asset_management.service.IRefreshTokenService;
import com.example.asset_management.service.ITokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final ITokenService tokenService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final IRefreshTokenService refreshTokenService;


    @PostMapping("/signin")
    public AuthResponse signIn(@Valid @RequestBody SignInRequest request) {
        return authService.signIn(request);
    }

    @PostMapping("/refresh")
    public AuthResponse generateAccessTokenFromRefreshToken(@RequestBody RefreshTokenRequest request) {
        String hashToken = refreshTokenService.hashToken(request.refreshToken());
        RefreshToken refreshToken = refreshTokenRepository.findByHashToken(hashToken)
                .map(refreshTokenService::verifyExpiration)
                .orElseThrow(() -> new HttpException("Invalid refresh token"));

        CustomUserDetails user = new CustomUserDetails(refreshToken.getAdmin());

        String accessToken = tokenService.generateAccessToken(user);

        return new AuthResponse(accessToken, request.refreshToken());
    }

    @PostMapping("/signout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest request) {
        String hashToken = refreshTokenService.hashToken(request.refreshToken());
        refreshTokenRepository.findByHashToken(hashToken)
                .ifPresent(refreshTokenRepository::delete);
        return ResponseEntity.noContent().build();
    }
}
