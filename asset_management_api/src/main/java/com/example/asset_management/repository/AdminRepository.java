package com.example.asset_management.repository;

import com.example.asset_management.model.Admin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AdminRepository extends JpaRepository<Admin, Long> {

    Optional<Admin> findByUserName(String userName);

    Optional<Admin> findByOauthProviderAndOauthSubject(String oauthProvider, String oauthSubject);
}
