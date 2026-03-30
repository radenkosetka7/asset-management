package com.example.asset_management.mapper;

import com.example.asset_management.dto.response.AssetAttributeValueResponse;
import com.example.asset_management.model.AssetAttributeValue;
import com.example.asset_management.model.elasticsearch.AssetValueDocument;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Component
public class AssetDocumentMapper {

    public List<AssetValueDocument> toDocumentDto(List<AssetAttributeValueResponse> attributeValueResponses) {
        return attributeValueResponses.stream()
                .map(this::toValueDocument).toList();
    }

    public List<AssetValueDocument> toDocumentEntity(List<AssetAttributeValue> attributeValue) {
        return attributeValue.stream()
                .map(this::toValueDocument).toList();
    }

    private AssetValueDocument toValueDocument(AssetAttributeValueResponse assetAttributeValue) {
        AssetValueDocument assetValueDocument = new AssetValueDocument();
        assetValueDocument.setAttributeId(assetAttributeValue.attribute().id());
        assetValueDocument.setDataType(assetAttributeValue.attribute().dataType().toString());
        assetValueDocument.setValueString(assetAttributeValue.valueString());
        assetValueDocument.setValueNumber(assetAttributeValue.valueNumber());
        assetValueDocument.setValueDate(toEpochMillis(assetAttributeValue.valueDate()));
        assetValueDocument.setValueBool(assetAttributeValue.valueBool());
        return assetValueDocument;
    }

    private AssetValueDocument toValueDocument(AssetAttributeValue assetAttributeValue) {
        AssetValueDocument assetValueDocument = new AssetValueDocument();
        assetValueDocument.setAttributeId(assetAttributeValue.getAttribute().getId());
        assetValueDocument.setDataType(String.valueOf(assetAttributeValue.getAttribute().getDataType()));
        assetValueDocument.setValueString(assetAttributeValue.getValueString());
        assetValueDocument.setValueNumber(assetAttributeValue.getValueNumber());
        assetValueDocument.setValueDate(toEpochMillis(assetAttributeValue.getValueDate()));
        assetValueDocument.setValueBool(assetAttributeValue.getValueBool());
        return assetValueDocument;
    }

    private Long toEpochMillis(LocalDate valueDate) {
        if (valueDate == null) {
            return null;
        }
        return valueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli();
    }

}
