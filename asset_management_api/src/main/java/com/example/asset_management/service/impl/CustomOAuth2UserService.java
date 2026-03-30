package com.example.asset_management.service.impl;

import com.example.asset_management.model.Admin;
import com.example.asset_management.model.CustomUserDetails;
import com.example.asset_management.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oAuth2User.getAttributes();

        String subject = extractSubject(provider, attrs);
        String email   = extractEmail(provider, attrs);
        String name    = extractName(provider, attrs);

        log.info("OAuth2 login: provider={}, subject={}, email={}", provider, subject, email);

        Admin admin = adminRepository
                .findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> createAdmin(provider, subject, email, name));

        if (email != null && !email.equals(admin.getEmail())) {
            admin.setEmail(email);
            adminRepository.save(admin);
        }

        return new CustomUserDetails(admin, oAuth2User.getAttributes());
    }


    private Admin createAdmin(String provider, String subject, String email, String fullName) {
        log.info("Auto-provisioning new admin for OAuth2 user: provider={}, subject={}", provider, subject);
        Admin admin = new Admin();
        admin.setOauthProvider(provider);
        admin.setOauthSubject(subject);
        admin.setEmail(email);

        String base = (fullName != null ? fullName.replaceAll("\\s+", ".").toLowerCase() : "user");
        String userName = base + "." + UUID.randomUUID().toString().substring(0, 6);
        admin.setUserName(userName);

        if (fullName != null && fullName.contains(" ")) {
            int idx = fullName.indexOf(' ');
            admin.setFirstName(fullName.substring(0, idx));
            admin.setLastName(fullName.substring(idx + 1));
        } else {
            admin.setFirstName(fullName != null ? fullName : "Unknown");
            admin.setLastName("-");
        }

        admin.setPassword(null);

        return adminRepository.save(admin);
    }

    private String extractSubject(String provider, Map<String, Object> attrs) {
        if ("github".equals(provider)) {
            Object id = attrs.get("id");
            return id != null ? String.valueOf(id) : null;
        }
        Object sub = attrs.get("sub");
        return sub != null ? String.valueOf(sub) : null;
    }

    private String extractEmail(String provider, Map<String, Object> attrs) {
        Object email = attrs.get("email");
        return email != null ? String.valueOf(email) : null;
    }

    private String extractName(String provider, Map<String, Object> attrs) {
        Object name = attrs.get("name");
        if (name != null) return String.valueOf(name);
        Object login = attrs.get("login");
        return login != null ? String.valueOf(login) : null;
    }
}

