package com.example.asset_management.service.impl;

import com.example.asset_management.model.Admin;
import com.example.asset_management.model.CustomUserDetails;
import com.example.asset_management.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final AdminRepository adminRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        String provider = userRequest.getClientRegistration().getRegistrationId();
        Map<String, Object> attrs = oidcUser.getAttributes();

        String subject = oidcUser.getSubject();
        String email   = oidcUser.getEmail();
        String name    = oidcUser.getFullName();

        log.info("OIDC login: provider={}, subject={}, email={}", provider, subject, email);

        Admin admin = adminRepository
                .findByOauthProviderAndOauthSubject(provider, subject)
                .orElseGet(() -> createAdmin(provider, subject, email, name));

        if (email != null && !email.equals(admin.getEmail())) {
            admin.setEmail(email);
            adminRepository.save(admin);
        }

        return new CustomUserDetails(admin, attrs, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }

    private Admin createAdmin(String provider, String subject, String email, String fullName) {
        log.info("Auto-provisioning admin for OIDC user: provider={}, subject={}", provider, subject);
        Admin admin = new Admin();
        admin.setOauthProvider(provider);
        admin.setOauthSubject(subject);
        admin.setEmail(email);

        String base = (fullName != null ? fullName.replaceAll("\\s+", ".").toLowerCase() : "user");
        admin.setUserName(base + "." + UUID.randomUUID().toString().substring(0, 6));

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
}
