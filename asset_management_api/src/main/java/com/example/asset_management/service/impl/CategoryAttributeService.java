package com.example.asset_management.service.impl;

import com.example.asset_management.dto.response.AttributeResponse;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.model.Attribute;
import com.example.asset_management.model.Category;
import com.example.asset_management.model.CategoryAttribute;
import com.example.asset_management.model.CategoryAttributeId;
import com.example.asset_management.repository.CategoryAttributeRepository;
import com.example.asset_management.service.ICategoryAttributeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategoryAttributeService implements ICategoryAttributeService {

    private final CategoryAttributeRepository categoryAttributeRepository;

    @Override
    public List<AttributeResponse> insert(Category category, List<Attribute> categoryAttributes) {
        log.info("Inserting attributes for category id: {}", category.getId());
        List<AttributeResponse> savedCategoryAttributes = new ArrayList<>();
        for (Attribute attribute : categoryAttributes) {
            CategoryAttributeId categoryAttributeId = new CategoryAttributeId();
            categoryAttributeId.setCategoryId(category.getId());
            categoryAttributeId.setAttributeId(attribute.getId());
            CategoryAttribute categoryAttribute = new CategoryAttribute();
            categoryAttribute.setId(categoryAttributeId);
            categoryAttribute.setCategory(category);
            categoryAttribute.setAttribute(attribute);
            categoryAttributeRepository.save(categoryAttribute);
            savedCategoryAttributes.add(AttributeResponse.from(attribute));

        }
        return savedCategoryAttributes;
    }

    @Override
    public List<AttributeResponse> getByCategoryId(Long id) {
        log.info("Getting attributes for category id: {}", id);
        List<CategoryAttribute> categoryAttribute = categoryAttributeRepository.findByCategory_Id(id);
        return categoryAttribute.stream().map(attr -> AttributeResponse.from(attr.getAttribute())).toList();
    }

    @Override
    public void delete(Long categoryId, Long attributeId) {
        log.info("Delete attribute for category: {} attributeId: {}", categoryId, attributeId);
        Optional<CategoryAttribute> categoryAttribute = categoryAttributeRepository.findByCategory_IdAndAttribute_Id(categoryId, attributeId);
        categoryAttribute.ifPresentOrElse(categoryAttributeRepository::delete, () -> {
            throw new NotFoundException("Attribute does not exist");
        });
    }
}
