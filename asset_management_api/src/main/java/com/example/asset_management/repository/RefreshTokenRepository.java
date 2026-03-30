package com.example.asset_management.repository;

import com.example.asset_management.model.Admin;
import com.example.asset_management.model.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByHashToken(String token);

    @Modifying
    @Transactional
    void deleteByAdmin(Admin admin);
}
