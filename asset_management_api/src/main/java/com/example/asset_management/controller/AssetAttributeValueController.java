package com.example.asset_management.controller;

import com.example.asset_management.service.IAssetAttributeValueService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/assets/attributes")
@RequiredArgsConstructor
public class AssetAttributeValueController {

    private final IAssetAttributeValueService assetAttributeValueService;

    @DeleteMapping("/{assetId}/{attributeId}")
    public ResponseEntity<Void> deleteAssetAttribute(@PathVariable Long assetId, @PathVariable Long attributeId) {
        assetAttributeValueService.delete(assetId, attributeId);
        return ResponseEntity.noContent().build();

    }

}
