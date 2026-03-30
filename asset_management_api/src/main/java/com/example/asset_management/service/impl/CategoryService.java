package com.example.asset_management.service.impl;

import com.example.asset_management.dto.request.CategoryRequest;
import com.example.asset_management.dto.response.AttributeResponse;
import com.example.asset_management.dto.response.CategoryResponse;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.model.Category;
import com.example.asset_management.repository.CategoryRepository;
import com.example.asset_management.service.IAttributeService;
import com.example.asset_management.service.ICategoryAttributeService;
import com.example.asset_management.service.ICategoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class CategoryService implements ICategoryService {

    private final CategoryRepository categoryRepository;
    private final IAttributeService attributeService;
    private final ICategoryAttributeService categoryAttributeService;

    @Override
    public CategoryResponse insertCategory(CategoryRequest request) {
        log.info("Inserting new category");
        Category.CategoryBuilder builder = Category.builder().
                description(request.description()).
                name(request.name());

        if (request.parentCategoryId() != null) {
            Category parent = categoryRepository.findById(request.parentCategoryId())
                    .orElseThrow(() -> new NotFoundException("Parent category not found id: " + request.parentCategoryId()));
            builder.parentCategory(parent);
        } else {
            builder.parentCategory(null);
        }
        Category category = builder.build();
        Category savedCategory = categoryRepository.save(category);
        List<AttributeResponse> attributeResponses = attributeService.insertList(savedCategory, request.attributeRequestList());
        return CategoryResponse.from(category, attributeResponses);


    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        log.info("Getting all categories");

        List<Category> categories = categoryRepository.findAll();
        List<CategoryResponse> responses = new ArrayList<>();
        for (Category category : categories) {
            responses.add(CategoryResponse.from(category, null));
        }
        return responses;
    }

    @Override
    public Optional<CategoryResponse> getCategoryById(Long id) {
        log.info("Get category id: {} ", id);
        List<AttributeResponse> attributeResponseList = categoryAttributeService.getByCategoryId(id);
        return categoryRepository.findById(id).map(category -> CategoryResponse.from(category, attributeResponseList));
    }
}
