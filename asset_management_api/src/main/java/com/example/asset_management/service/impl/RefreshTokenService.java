package com.example.asset_management.service.impl;

import com.example.asset_management.exception.HttpException;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.model.Admin;
import com.example.asset_management.model.RefreshToken;
import com.example.asset_management.repository.AdminRepository;
import com.example.asset_management.repository.RefreshTokenRepository;
import com.example.asset_management.service.IRefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService implements IRefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AdminRepository adminRepository;

    @Value("${authorization.token.refresh-secret}")
    private String refreshTokenSecret;

    @Value("${authorization.refresh-token.expiration-time}")
    private long refreshTokenExpirationTime;

    @Override
    @Transactional
    public String createRefreshToken(UserDetails user) {
        log.info("Generating refresh token for user: {}", user.getUsername());
        Admin admin = adminRepository.findByUserName(user.getUsername())
                .orElseThrow(() -> {
                    log.info("Admin: {} does not exist.", user.getUsername());
                    return new NotFoundException("Admin " + user.getUsername() + " does not exist.");
                });
        refreshTokenRepository.deleteByAdmin(admin);
        String rawToken = UUID.randomUUID().toString();
        RefreshToken refreshToken = new RefreshToken();
        String hashToken = hashToken(rawToken);
        refreshToken.setHashToken(hashToken);
        refreshToken.setAdmin(admin);
        refreshToken.setExpiryDate(Instant.now().plusMillis(refreshTokenExpirationTime));
        refreshTokenRepository.save(refreshToken);
        return rawToken;
    }

    @Override
    public RefreshToken verifyExpiration(RefreshToken refreshToken) {
        log.info("Checking refresh token expiration");

        if (refreshToken.getExpiryDate().isBefore(Instant.now())) {
            log.info("Token {} expired, deleting from database...", refreshToken);
            refreshTokenRepository.delete(refreshToken);
            throw new HttpException("Refresh token expired");
        }
        return refreshToken;
    }

    @Override
    public String hashToken(String token) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec key = new SecretKeySpec(refreshTokenSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(key);
            byte[] hash = mac.doFinal(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            log.error("Failed to hash token: {}", e.getMessage());
            throw new HttpException("Failed to hash token");
        }
    }
}
