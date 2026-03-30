package com.example.asset_management.service;

import com.example.asset_management.dto.request.SignInRequest;
import com.example.asset_management.dto.response.AuthResponse;

public interface IAuthService {

    AuthResponse signIn(SignInRequest request);
}
