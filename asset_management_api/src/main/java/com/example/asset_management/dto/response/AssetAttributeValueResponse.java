package com.example.asset_management.dto.response;

import com.example.asset_management.model.AssetAttributeValue;
import jakarta.annotation.Nullable;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;


public record AssetAttributeValueResponse(AttributeResponse attribute, @Nullable String valueString,
                                          @Nullable BigDecimal valueNumber, @Nullable
                                          LocalDate valueDate, @Nullable Boolean valueBool) implements Serializable {

    public static AssetAttributeValueResponse from(AttributeResponse attributeResponse, AssetAttributeValue assetAttributeValue) {
        return new AssetAttributeValueResponse(attributeResponse, assetAttributeValue.getValueString(), assetAttributeValue.getValueNumber(),
                assetAttributeValue.getValueDate(), assetAttributeValue.getValueBool());
    }
}
