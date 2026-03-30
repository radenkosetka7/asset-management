package com.example.asset_management.repository;

import com.example.asset_management.model.AssetAttributeValue;
import com.example.asset_management.model.AssetAttributeValueId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AssetAttributeValueRepository extends JpaRepository<AssetAttributeValue, AssetAttributeValueId> {

    Optional<AssetAttributeValue> findByAsset_IdAndAttribute_Id(Long assetId, Long attributeId);

    List<AssetAttributeValue> findByAsset_Id(Long assetId);

    @Query("""
    select v
    from AssetAttributeValue v
    join fetch v.attribute
    where v.asset.id in :assetIds
""")
    List<AssetAttributeValue> findAllWithAttributeByAssetIds(
            @Param("assetIds") List<Long> assetIds
    );


}
