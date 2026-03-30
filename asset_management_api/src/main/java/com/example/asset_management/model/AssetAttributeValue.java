package com.example.asset_management.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;


@Entity
@Data
@Table(name = "asset_attribute_value")
public class AssetAttributeValue {
    @SequenceGenerator(name = "asset_attribute_value_id_gen", sequenceName = "attribute_id_seq", allocationSize = 1)
    @EmbeddedId
    private AssetAttributeValueId id;

    @MapsId("assetId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private com.example.asset_management.model.Attribute attribute;

    @Column(name = "value_string")
    private String valueString;

    @Column(name = "value_number", precision = 18, scale = 2)
    private BigDecimal valueNumber;

    @Column(name = "value_date")
    private LocalDate valueDate;

    @Column(name = "value_bool")
    private Boolean valueBool;

}