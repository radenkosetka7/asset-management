package com.example.asset_management.service;

import com.example.asset_management.dto.response.AttributeResponse;
import com.example.asset_management.model.Attribute;
import com.example.asset_management.model.Category;

import java.util.List;

public interface ICategoryAttributeService {

    List<AttributeResponse> insert(Category category, List<Attribute> categoryAttributes);

    List<AttributeResponse> getByCategoryId(Long id);

    void delete(Long categoryId, Long attributeId);
}
