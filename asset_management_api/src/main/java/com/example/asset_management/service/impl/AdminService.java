package com.example.asset_management.service.impl;

import com.example.asset_management.model.Admin;
import com.example.asset_management.repository.AdminRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    public void postConstruct() {
        if (adminRepository.count() == 0) {
            Admin admin1 = new Admin();
            admin1.setFirstName("Admin1");
            admin1.setLastName("Admin1");
            admin1.setUserName("admin1");
            admin1.setPassword(passwordEncoder.encode("admin1"));

            Admin admin2 = new Admin();
            admin2.setFirstName("Admin2");
            admin2.setLastName("Admin2");
            admin2.setUserName("admin2");
            admin2.setPassword(passwordEncoder.encode("admin2"));

            adminRepository.saveAndFlush(admin1);
            adminRepository.saveAndFlush(admin2);
        }
    }
}
