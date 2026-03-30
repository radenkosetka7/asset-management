package com.example.asset_management.service;


import org.springframework.security.core.userdetails.UserDetails;

public interface ITokenService {

    String generateAccessToken(UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);


    String extractUsername(String token);

    boolean isTokenValid(String token, UserDetails userDetails);
}
