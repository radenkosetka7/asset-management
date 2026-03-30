package com.example.asset_management.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "asset_image")
public class AssetImage {
    @SequenceGenerator(name = "asset_image_id_gen", sequenceName = "attribute_id_seq", allocationSize = 1)
    @EmbeddedId
    private AssetImageId id;

    @MapsId("assetId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @MapsId("imageId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "image_id", nullable = false)
    private com.example.asset_management.model.Image image;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;
}