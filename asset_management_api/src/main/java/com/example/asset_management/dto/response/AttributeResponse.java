package com.example.asset_management.dto.response;

import com.example.asset_management.dto.enumerator.AttributeDataType;
import com.example.asset_management.model.Attribute;

import java.io.Serializable;


public record AttributeResponse(Long id, String name, AttributeDataType dataType) implements Serializable {

    public static AttributeResponse from(Attribute attribute) {
        return new AttributeResponse(attribute.getId(), attribute.getName(), attribute.getDataType());
    }
}
