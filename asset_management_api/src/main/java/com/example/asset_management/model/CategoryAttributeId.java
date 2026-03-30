package com.example.asset_management.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import org.hibernate.Hibernate;

import java.util.Objects;

@Data
@Embeddable
public class CategoryAttributeId implements java.io.Serializable {
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Column(name = "attribute_id", nullable = false)
    private Long attributeId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CategoryAttributeId entity = (CategoryAttributeId) o;
        return Objects.equals(this.attributeId, entity.attributeId) &&
                Objects.equals(this.categoryId, entity.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(attributeId, categoryId);
    }

}