package com.example.asset_management.model;

import com.example.asset_management.dto.enumerator.AssetStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Table(name = "asset")
public class Asset {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "asset_id_gen")
    @SequenceGenerator(name = "asset_id_gen", sequenceName = "asset_id_seq", allocationSize = 1)
    @Column(name = "asset_id", nullable = false)
    private Long id;

    @Column(name = "asset_code", nullable = false, length = 50, unique = true)
    private String assetCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private AssetStatus status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "modified_at")
    private Instant modifiedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private com.example.asset_management.model.Category category;

    @OneToMany(mappedBy = "asset", fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("sortOrder ASC")
    private Set<AssetImage> assetImages = new LinkedHashSet<>();


}