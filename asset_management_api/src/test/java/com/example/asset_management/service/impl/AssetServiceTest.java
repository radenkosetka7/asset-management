package com.example.asset_management.service.impl;

import com.example.asset_management.dto.enumerator.AssetStatus;
import com.example.asset_management.dto.request.AssetRequest;
import com.example.asset_management.dto.response.*;
import com.example.asset_management.exception.NotFoundException;
import com.example.asset_management.mapper.AssetDocumentMapper;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.Category;
import com.example.asset_management.model.elasticsearch.AssetDocument;
import com.example.asset_management.model.elasticsearch.AssetValueDocument;
import com.example.asset_management.repository.*;
import com.example.asset_management.service.IAssetAttributeValueService;
import com.example.asset_management.service.IImageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private AssetRepository assetRepository;

    @Mock
    private IImageService imageService;

    @Mock
    private IAssetAttributeValueService attributeValueService;

    @Mock
    private AssetImageRepository assetImageRepository;

    @Mock
    private AssetAttributeValueRepository assetAttributeValueRepository;

    @Mock
    private AssetDocumentRepository assetDocumentRepository;


    @Mock
    private AssetDocumentMapper assetDocumentMapper;

    @InjectMocks
    private AssetService assetService;

    @Test
    void insertAssetWithValidDataShouldCreateAssetSuccessfully() {
        Long categoryId = 1L;
        Category category = Category.builder()
                .id(categoryId)
                .name("Electronics")
                .build();

        AssetRequest request = new AssetRequest(
                "AST-001",
                "Laptop",
                "Dell XPS 15",
                null,
                categoryId,
                null
        );

        Asset savedAsset = Asset.builder()
                .id(1L)
                .assetCode("AST-001")
                .name("Laptop")
                .description("Dell XPS 15")
                .status(AssetStatus.ACTIVE)
                .category(category)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        List<MultipartFile> files = Collections.emptyList();
        Set<ImageResponse> images = new LinkedHashSet<>();
        List<AssetAttributeValueResponse> attributeValues = Collections.emptyList();
        List<AssetValueDocument> documentValues = Collections.emptyList();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);
        when(imageService.insert(any(Asset.class), anyList())).thenReturn(images);
        when(attributeValueService.insert(any(Asset.class), any())).thenReturn(attributeValues);
        when(assetDocumentMapper.toDocumentDto(anyList())).thenReturn(documentValues);
        when(assetDocumentRepository.save(any(AssetDocument.class))).thenReturn(null);

        AssetResponse response = assetService.insert(request, files);

        assertNotNull(response);
        assertEquals("AST-001", response.assetCode());
        assertEquals("Laptop", response.name());
        assertEquals("Dell XPS 15", response.description());
        assertEquals(AssetStatus.ACTIVE, response.assetStatus());
        verify(categoryRepository).findById(categoryId);
        verify(assetRepository).save(any(Asset.class));
        verify(imageService).insert(any(Asset.class), eq(files));
        verify(assetDocumentRepository).save(any(AssetDocument.class));
    }

    @Test
    void insertAssetWithNonExistentCategoryShouldThrowNotFoundException() {
        Long categoryId = 999L;
        AssetRequest request = new AssetRequest(
                "AST-002",
                "Monitor",
                "Samsung 27\"",
                null,
                categoryId,
                null
        );

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> assetService.insert(request, Collections.emptyList()));
        verify(categoryRepository).findById(categoryId);
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void insertAssetWithNullFilesShouldHandleGracefully() {
        Long categoryId = 1L;
        Category category = Category.builder().id(categoryId).name("Furniture").build();

        AssetRequest request = new AssetRequest(
                "AST-003",
                "Desk",
                "Standing Desk",
                AssetStatus.ACTIVE,
                categoryId,
                null
        );

        Asset savedAsset = Asset.builder()
                .id(3L)
                .assetCode("AST-003")
                .name("Desk")
                .description("Standing Desk")
                .status(AssetStatus.ACTIVE)
                .category(category)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);
        when(imageService.insert(any(Asset.class), isNull())).thenReturn(new LinkedHashSet<>());
        when(attributeValueService.insert(any(Asset.class), any())).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentDto(anyList())).thenReturn(Collections.emptyList());

        AssetResponse response = assetService.insert(request, null);

        assertNotNull(response);
        verify(imageService).insert(any(Asset.class), isNull());
    }

    @Test
    void getByIdWithExistingAssetShouldReturnAssetResponse() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .description("Dell XPS")
                .status(AssetStatus.ACTIVE)
                .category(category)
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetImageRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());

        Optional<AssetResponse> response = assetService.getById(assetId);

        assertTrue(response.isPresent());
        assertEquals(assetId, response.get().id());
        assertEquals("AST-001", response.get().assetCode());
        assertEquals("Laptop", response.get().name());
        verify(assetRepository).findById(assetId);
    }

    @Test
    void getByIdWithNonExistentAssetShouldReturnEmptyOptional() {
        Long assetId = 999L;

        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());
        when(assetImageRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());

        Optional<AssetResponse> response = assetService.getById(assetId);

        assertFalse(response.isPresent());
        verify(assetRepository).findById(assetId);
    }

    @Test
    void getAllShouldReturnPagedAssets() {
        Pageable pageable = PageRequest.of(0, 10);
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset1 = Asset.builder().id(1L).assetCode("AST-001").name("Laptop").category(category).status(AssetStatus.ACTIVE).build();
        Asset asset2 = Asset.builder().id(2L).assetCode("AST-002").name("Monitor").category(category).status(AssetStatus.ACTIVE).build();

        Page<Asset> assetPage = new PageImpl<>(List.of(asset1, asset2), pageable, 2);

        when(assetRepository.findAll(pageable)).thenReturn(assetPage);
        when(assetImageRepository.findByAsset_Id(anyLong())).thenReturn(Collections.emptyList());

        Page<AssetResponse> response = assetService.getAll(pageable);

        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        assertEquals(2, response.getContent().size());
        verify(assetRepository).findAll(pageable);
    }

    @Test
    void getAllWithEmptyDatabaseShouldReturnEmptyPage() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Asset> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(assetRepository.findAll(pageable)).thenReturn(emptyPage);

        Page<AssetResponse> response = assetService.getAll(pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
        assertTrue(response.getContent().isEmpty());
    }

    @Test
    void deleteExistingAssetShouldSetStatusToRetired() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .status(AssetStatus.ACTIVE)
                .category(category)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentEntity(anyList())).thenReturn(Collections.emptyList());
        when(assetDocumentRepository.save(any(AssetDocument.class))).thenReturn(null);

        assetService.delete(assetId);

        verify(assetRepository).findById(assetId);
        verify(assetRepository).save(argThat(a -> a.getStatus() == AssetStatus.RETIRED));
        verify(assetDocumentRepository).save(any(AssetDocument.class));
    }

    @Test
    void deleteNonExistentAssetShouldThrowNotFoundException() {
        Long assetId = 999L;

        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> assetService.delete(assetId));
        verify(assetRepository).findById(assetId);
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void deleteAlreadyRetiredAssetShouldStillUpdateTimestamp() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .status(AssetStatus.RETIRED)
                .category(category)
                .createdAt(Instant.now().minusSeconds(7200))
                .modifiedAt(Instant.now().minusSeconds(3600))
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentEntity(anyList())).thenReturn(Collections.emptyList());

        assetService.delete(assetId);

        verify(assetRepository).save(argThat(a ->
            a.getStatus() == AssetStatus.RETIRED &&
            a.getModifiedAt().isAfter(Instant.now().minusSeconds(5))
        ));
    }

    @Test
    void updateExistingAssetShouldModifyAllFields() {
        Long assetId = 1L;
        Long newCategoryId = 2L;
        Category oldCategory = Category.builder().id(1L).name("Electronics").build();
        Category newCategory = Category.builder().id(newCategoryId).name("Furniture").build();

        Asset existingAsset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .description("Old description")
                .status(AssetStatus.ACTIVE)
                .category(oldCategory)
                .createdAt(Instant.now().minusSeconds(3600))
                .modifiedAt(Instant.now().minusSeconds(1800))
                .build();

        AssetRequest updateRequest = new AssetRequest(
                "AST-001-UPDATED",
                "Desktop",
                "New description",
                AssetStatus.IN_USE,
                newCategoryId,
                null
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(existingAsset));
        when(categoryRepository.findById(newCategoryId)).thenReturn(Optional.of(newCategory));
        when(assetRepository.save(any(Asset.class))).thenReturn(existingAsset);
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentEntity(anyList())).thenReturn(Collections.emptyList());
        when(assetDocumentRepository.save(any(AssetDocument.class))).thenReturn(null);

        AssetResponse response = assetService.update(assetId, updateRequest);

        assertNotNull(response);
        verify(assetRepository).save(argThat(asset ->
                asset.getAssetCode().equals("AST-001-UPDATED") &&
                asset.getName().equals("Desktop") &&
                asset.getDescription().equals("New description") &&
                asset.getStatus() == AssetStatus.IN_USE &&
                asset.getCategory().equals(newCategory)
        ));
    }

    @Test
    void updateAssetWithNullStatusShouldKeepExistingStatus() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();

        Asset existingAsset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .status(AssetStatus.IN_USE)
                .category(category)
                .createdAt(Instant.now().minusSeconds(3600))
                .modifiedAt(Instant.now().minusSeconds(1800))
                .build();

        AssetRequest updateRequest = new AssetRequest(
                "AST-001",
                "Laptop Pro",
                "Updated laptop",
                null,
                1L,
                null
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(existingAsset));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(assetRepository.save(any(Asset.class))).thenReturn(existingAsset);
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentEntity(anyList())).thenReturn(Collections.emptyList());

        assetService.update(assetId, updateRequest);

        verify(assetRepository).save(argThat(asset -> asset.getStatus() == AssetStatus.IN_USE));
    }

    @Test
    void updateNonExistentAssetShouldThrowNotFoundException() {
        Long assetId = 999L;
        AssetRequest updateRequest = new AssetRequest(
                "AST-999",
                "NonExistent",
                "Description",
                null,
                1L,
                null
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> assetService.update(assetId, updateRequest));
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void updateAssetWithInvalidCategoryShouldThrowNotFoundException() {
        Long assetId = 1L;
        Long invalidCategoryId = 999L;
        Category oldCategory = Category.builder().id(1L).name("Electronics").build();

        Asset existingAsset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .category(oldCategory)
                .build();

        AssetRequest updateRequest = new AssetRequest(
                "AST-001",
                "Laptop",
                "Description",
                null,
                invalidCategoryId,
                null
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(existingAsset));
        when(categoryRepository.findById(invalidCategoryId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> assetService.update(assetId, updateRequest));
        verify(assetRepository, never()).save(any(Asset.class));
    }

    @Test
    void getAllByCategoryIdShouldReturnFilteredAssets() {
        Long categoryId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        Category category = Category.builder().id(categoryId).name("Electronics").build();

        Asset asset1 = Asset.builder().id(1L).assetCode("AST-001").name("Laptop").category(category).status(AssetStatus.ACTIVE).build();
        Asset asset2 = Asset.builder().id(2L).assetCode("AST-002").name("Monitor").category(category).status(AssetStatus.ACTIVE).build();

        Page<Asset> assetPage = new PageImpl<>(List.of(asset1, asset2), pageable, 2);

        when(assetRepository.findAssetByCategory_Id(categoryId, pageable)).thenReturn(assetPage);
        when(assetImageRepository.findByAsset_Id(anyLong())).thenReturn(Collections.emptyList());

        Page<AssetResponse> response = assetService.getAllByCategoryId(categoryId, pageable);

        assertNotNull(response);
        assertEquals(2, response.getTotalElements());
        verify(assetRepository).findAssetByCategory_Id(categoryId, pageable);
    }

    @Test
    void getAllByCategoryIdWithNonExistentCategoryShouldReturnEmptyPage() {
        Long categoryId = 999L;
        Pageable pageable = PageRequest.of(0, 10);
        Page<Asset> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

        when(assetRepository.findAssetByCategory_Id(categoryId, pageable)).thenReturn(emptyPage);

        Page<AssetResponse> response = assetService.getAllByCategoryId(categoryId, pageable);

        assertNotNull(response);
        assertEquals(0, response.getTotalElements());
    }


    @Test
    void getByIdShouldCacheResult() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .category(category)
                .status(AssetStatus.ACTIVE)
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetImageRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());

        assetService.getById(assetId);

        verify(assetRepository, times(1)).findById(assetId);
    }

    @Test
    void updateShouldEvictCache() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .category(category)
                .status(AssetStatus.ACTIVE)
                .createdAt(Instant.now().minusSeconds(3600))
                .modifiedAt(Instant.now().minusSeconds(1800))
                .build();

        AssetRequest updateRequest = new AssetRequest(
                "AST-001-UPDATED",
                "Updated Laptop",
                "New description",
                null,
                1L,
                null
        );

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentEntity(anyList())).thenReturn(Collections.emptyList());

        assetService.update(assetId, updateRequest);

        verify(assetRepository).save(any(Asset.class));
    }

    @Test
    void deleteShouldEvictCache() {
        Long assetId = 1L;
        Category category = Category.builder().id(1L).name("Electronics").build();
        Asset asset = Asset.builder()
                .id(assetId)
                .assetCode("AST-001")
                .name("Laptop")
                .category(category)
                .status(AssetStatus.ACTIVE)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        when(assetRepository.findById(assetId)).thenReturn(Optional.of(asset));
        when(assetRepository.save(any(Asset.class))).thenReturn(asset);
        when(assetAttributeValueRepository.findByAsset_Id(assetId)).thenReturn(Collections.emptyList());
        when(assetDocumentMapper.toDocumentEntity(anyList())).thenReturn(Collections.emptyList());

        assetService.delete(assetId);

        verify(assetRepository).save(argThat(a -> a.getStatus() == AssetStatus.RETIRED));
    }
}

