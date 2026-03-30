package com.example.asset_management.model.elasticsearch;

import com.example.asset_management.dto.enumerator.AssetStatus;
import com.example.asset_management.model.Asset;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.Instant;
import java.util.List;

@Document(indexName = "assets")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssetDocument {

    @Id
    private Long id;
    private String assetCode;
    private String name;
    private String description;
    private AssetStatus status;
    private Long createdAt;
    private Long modifiedAt;
    private String categoryName;

    @Field(type = FieldType.Nested)
    private List<AssetValueDocument> attributes;


    public static AssetDocument from(Asset asset, List<AssetValueDocument> assetValueDocuments) {
        return new AssetDocument(asset.getId(), asset.getAssetCode(), asset.getName(),
                asset.getDescription(), asset.getStatus(), asset.getCreatedAt().toEpochMilli(),
                asset.getModifiedAt().toEpochMilli(), asset.getCategory().getName(), assetValueDocuments);
    }

}
