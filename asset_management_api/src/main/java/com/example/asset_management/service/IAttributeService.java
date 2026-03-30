package com.example.asset_management.service;

import com.example.asset_management.dto.request.AttributeRequest;
import com.example.asset_management.dto.response.AttributeResponse;
import com.example.asset_management.model.Category;

import java.util.List;

public interface IAttributeService {

    List<AttributeResponse> insertList(Category category, List<AttributeRequest> attributeRequestList);
}
