package com.example.asset_management.service;

import com.example.asset_management.dto.request.CategoryRequest;
import com.example.asset_management.dto.response.CategoryResponse;

import java.util.List;
import java.util.Optional;

public interface ICategoryService {

    CategoryResponse insertCategory(CategoryRequest request);

    List<CategoryResponse> getAllCategories();

    Optional<CategoryResponse> getCategoryById(Long id);

}
