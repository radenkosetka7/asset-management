package com.example.asset_management.service.impl;

import com.example.asset_management.dto.request.AttributeRequest;
import com.example.asset_management.dto.response.AttributeResponse;
import com.example.asset_management.model.Attribute;
import com.example.asset_management.model.Category;
import com.example.asset_management.repository.AttributeRepository;
import com.example.asset_management.service.IAttributeService;
import com.example.asset_management.service.ICategoryAttributeService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AttributeService implements IAttributeService {

    private final AttributeRepository attributeRepository;
    private final ICategoryAttributeService categoryAttributeService;

    @Override
    public List<AttributeResponse> insertList(Category category, List<AttributeRequest> attributeRequestList) {
        log.info("Inserting list of attributes");
        List<Attribute> attributes = attributeRequestList.stream()
                .map(attribute -> Attribute.builder()
                        .name(attribute.name())
                        .dataType(attribute.dataType())
                        .build()
                ).toList();

        List<Attribute> responses = attributeRepository.saveAll(attributes);
        return categoryAttributeService.insert(category, responses);
    }
}
