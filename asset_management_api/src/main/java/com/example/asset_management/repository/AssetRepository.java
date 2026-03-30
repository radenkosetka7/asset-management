package com.example.asset_management.repository;

import com.example.asset_management.model.Asset;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AssetRepository extends JpaRepository<Asset, Long> {

    Page<Asset> findAssetByCategory_Id(Long categoryId, Pageable pageable);

    List<Asset> findByIdIn(List<Long> ids);

    @Query(value = """
            SELECT DISTINCT a
                FROM Asset a 
                    LEFT JOIN FETCH a.assetImages ai
                        LEFT JOIN FETCH ai.image""",
            countQuery = "SELECT COUNT(DISTINCT a) FROM Asset a")
    Page<Asset> getAllAssetsWithImages(Pageable pageable);
}
