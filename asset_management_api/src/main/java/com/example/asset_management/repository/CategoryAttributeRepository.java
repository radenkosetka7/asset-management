package com.example.asset_management.repository;

import com.example.asset_management.model.CategoryAttribute;
import com.example.asset_management.model.CategoryAttributeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryAttributeRepository extends JpaRepository<CategoryAttribute, CategoryAttributeId> {

    @Query("SELECT a FROM CategoryAttribute a " +
            "WHERE a.category.id=:categoryId OR a.category.id= (" +
                "SELECT c.parentCategory.id " +
                "FROM Category c WHERE c.id=:categoryId" +
                ")")
    List<CategoryAttribute> findByCategory_Id(Long categoryId);

    Optional<CategoryAttribute> findByCategory_IdAndAttribute_Id(Long categoryId, Long attributeId);
}
