package com.example.asset_management.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
@Table(name = "category_attribute")
public class CategoryAttribute {
    @SequenceGenerator(name = "category_attribute_id_gen", sequenceName = "image_image_id_seq", allocationSize = 1)
    @EmbeddedId
    private CategoryAttributeId id;

    @MapsId("categoryId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private Attribute attribute;

}