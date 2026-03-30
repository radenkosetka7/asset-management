package com.example.asset_management.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CustomUserDetails implements UserDetails, OidcUser {

    private final Admin admin;
    private final Map<String, Object> oauth2Attributes;
    private final OidcIdToken idToken;
    private final OidcUserInfo userInfo;

    public CustomUserDetails(Admin admin) {
        this.admin = admin;
        this.oauth2Attributes = Map.of();
        this.idToken = null;
        this.userInfo = null;
    }

    public CustomUserDetails(Admin admin, Map<String, Object> oauth2Attributes) {
        this.admin = admin;
        this.oauth2Attributes = oauth2Attributes != null ? oauth2Attributes : Map.of();
        this.idToken = null;
        this.userInfo = null;
    }

    public CustomUserDetails(Admin admin, Map<String, Object> oauth2Attributes,
                             OidcIdToken idToken, OidcUserInfo userInfo) {
        this.admin = admin;
        this.oauth2Attributes = oauth2Attributes != null ? oauth2Attributes : Map.of();
        this.idToken = idToken;
        this.userInfo = userInfo;
    }

    public Admin getAdmin() { return admin; }

    @Override public OidcIdToken getIdToken()   { return idToken; }
    @Override public OidcUserInfo getUserInfo() { return userInfo; }
    @Override public Map<String, Object> getClaims() {
        return idToken != null ? idToken.getClaims() : oauth2Attributes;
    }

    @Override public Map<String, Object> getAttributes() { return oauth2Attributes; }
    @Override public String getName() { return admin.getUserName(); }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    @Override public String getPassword()  { return admin.getPassword(); }
    @Override public String getUsername()  { return admin.getUserName(); }
    @Override public boolean isAccountNonExpired()     { return true; }
    @Override public boolean isAccountNonLocked()      { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled()               { return true; }
}
