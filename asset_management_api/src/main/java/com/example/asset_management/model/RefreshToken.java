package com.example.asset_management.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Data
@Table(name = "refresh_token")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "refresh_token_id_gen")
    @SequenceGenerator(name = "refresh_token_id_gen", sequenceName = "refresh_token_refresh_token_id_seq", allocationSize = 1)
    @Column(name = "refresh_token_id", nullable = false)
    private Long id;

    @Column(name = "hash_token", nullable = false, length = 44, unique = true)
    private String hashToken;

    @OneToOne
    @JoinColumn(name = "admin_id", nullable = false)
    private Admin admin;

    @Column(name = "expiry_date")
    private Instant expiryDate;

}
