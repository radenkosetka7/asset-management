package com.example.asset_management.repository;

import com.example.asset_management.model.elasticsearch.AssetDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

public interface AssetDocumentRepository extends ElasticsearchRepository<AssetDocument, Long> {
}
