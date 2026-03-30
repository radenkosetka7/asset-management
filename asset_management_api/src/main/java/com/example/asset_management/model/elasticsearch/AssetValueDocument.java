package com.example.asset_management.model.elasticsearch;

import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
public class AssetValueDocument {

    private Long attributeId;
    private String dataType;
    private String valueString;
    private BigDecimal valueNumber;
    private Long valueDate;
    private Boolean valueBool;
}
