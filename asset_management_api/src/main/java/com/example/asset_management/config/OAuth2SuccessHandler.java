package com.example.asset_management.config;

import com.example.asset_management.model.CustomUserDetails;
import com.example.asset_management.service.IRefreshTokenService;
import com.example.asset_management.service.ITokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final ITokenService tokenService;
    private final IRefreshTokenService refreshTokenService;

    @Value("${app.oauth2.redirect-uri:http://localhost:4200/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        String accessToken  = tokenService.generateAccessToken(userDetails);
        String refreshToken = refreshTokenService.createRefreshToken(userDetails);
        log.info("OAuth2 login succeeded for user: {}", userDetails.getUsername());

        String encodedAccess  = URLEncoder.encode(accessToken,  StandardCharsets.UTF_8);
        String encodedRefresh = URLEncoder.encode(refreshToken, StandardCharsets.UTF_8);
        String targetUrl = redirectUri + "?accessToken=" + encodedAccess + "&refreshToken=" + encodedRefresh;

        clearAuthenticationAttributes(request);

        response.setContentType("text/html;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(
            "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
            "<script>window.location.replace('" + targetUrl + "');</script>" +
            "</head><body>Redirecting…</body></html>"
        );
        response.getWriter().flush();
    }
}

