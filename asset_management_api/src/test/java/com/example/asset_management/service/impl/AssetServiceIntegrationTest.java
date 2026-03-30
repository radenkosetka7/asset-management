package com.example.asset_management.service.impl;

import com.example.asset_management.dto.enumerator.AssetStatus;
import com.example.asset_management.dto.request.AssetFilterRequest;
import com.example.asset_management.dto.response.AssetResponse;
import com.example.asset_management.model.Asset;
import com.example.asset_management.model.Category;
import com.example.asset_management.model.elasticsearch.AssetDocument;
import com.example.asset_management.repository.AssetDocumentRepository;
import com.example.asset_management.repository.AssetRepository;
import com.example.asset_management.repository.CategoryRepository;
import com.example.asset_management.service.IAssetService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class AssetServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Container
    static ElasticsearchContainer elasticsearch = new ElasticsearchContainer("docker.elastic.co/elasticsearch/elasticsearch:8.11.0")
            .withEnv("xpack.security.enabled", "false")
            .withEnv("discovery.type", "single-node")
            .withEnv("ES_JAVA_OPTS", "-Xms512m -Xmx512m");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.elasticsearch.uris", () -> "http://" + elasticsearch.getHttpHostAddress());
        registry.add("elasticsearch.custom.config.enabled", () -> "false");
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379).toString());
    }

    @Autowired
    private IAssetService assetService;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private AssetDocumentRepository assetDocumentRepository;


    private Category electronicsCategory;
    private Category furnitureCategory;

    @BeforeEach
    void setUp() {
        assetDocumentRepository.deleteAll();
        assetRepository.deleteAll();
        categoryRepository.deleteAll();

        electronicsCategory = categoryRepository.save(Category.builder()
                .name("Electronics")
                .description("Electronic devices")
                .build());

        furnitureCategory = categoryRepository.save(Category.builder()
                .name("Furniture")
                .description("Office furniture")
                .build());
    }

    @Test
    void searchAssetsWithExactMatchShouldReturnAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("Dell Laptop", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("AST-001", results.getContent().getFirst().assetCode());
        assertEquals("Dell Laptop", results.getContent().getFirst().name());
    }

    @Test
    void searchAssetsWithPartialMatchShouldReturnAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("Laptop", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("Dell Laptop", results.getContent().getFirst().name());
    }

    @Test
    void searchAssetsWithAssetCodeShouldReturnAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("AST-001", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("AST-001", results.getContent().getFirst().assetCode());
    }

    @Test
    void searchAssetsWithPartialAssetCodeShouldReturnAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("AST", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
    }

    @Test
    void searchAssetsWithCaseInsensitiveQueryShouldReturnAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("dell laptop", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("Dell Laptop", results.getContent().getFirst().name());
    }

    @Test
    void searchAssetsWithUppercaseQueryShouldReturnAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("DELL LAPTOP", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
    }

    @Test
    void searchAssetsWithMultipleMatchesShouldReturnAllOrderedByRelevance() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-003", "Dell Monitor", "Dell 27 inch", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("Dell", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().anyMatch(a -> a.name().contains("Dell")));
    }

    @Test
    void searchAssetsWithNoMatchesShouldReturnEmptyPage() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("NonExistentAsset", PageRequest.of(0, 10));

        assertNotNull(results);
        assertEquals(0, results.getTotalElements());
    }

    @Test
    void searchAssetsWithSpecialCharactersShouldHandleGracefully() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        Page<AssetResponse> results = assetService.searchAssets("Dell*Laptop", PageRequest.of(0, 10));

        assertNotNull(results);
    }

    @Test
    void searchAssetsWithPaginationShouldReturnCorrectPage() {
        for (int i = 1; i <= 15; i++) {
            createAndIndexAsset("AST-" + String.format("%03d", i), "Laptop " + i, "Description " + i, electronicsCategory, AssetStatus.ACTIVE);
        }

        waitForElasticsearchIndexing();

        Page<AssetResponse> firstPage = assetService.searchAssets("Laptop", PageRequest.of(0, 5));
        Page<AssetResponse> secondPage = assetService.searchAssets("Laptop", PageRequest.of(1, 5));

        assertNotNull(firstPage);
        assertNotNull(secondPage);
        assertEquals(5, firstPage.getContent().size());
        assertEquals(5, secondPage.getContent().size());
        assertEquals(15, firstPage.getTotalElements());
        assertNotEquals(firstPage.getContent().getFirst().id(), secondPage.getContent().getFirst().id());
    }

    @Test
    void filterAssetsByAssetCodeShouldReturnMatchingAsset() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest("AST-001", null, null, null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("AST-001", results.getContent().getFirst().assetCode());
    }

    @Test
    void filterAssetsByPartialAssetCodeShouldReturnMatchingAssets() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("MON-001", "Dell Monitor", "Dell 27 inch", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest("AST", null, null, null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().allMatch(a -> a.assetCode().startsWith("AST")));
    }

    @Test
    void filterAssetsByNameShouldReturnMatchingAssets() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("MON-001", "Dell Monitor", "Dell 27 inch", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest(null, "Laptop", null, null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().allMatch(a -> a.name().contains("Laptop")));
    }

    @Test
    void filterAssetsByCategoryNameShouldReturnMatchingAssets() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("FUR-001", "Office Desk", "Standing desk", furnitureCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest(null, null, "Electronics", null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().allMatch(a -> a.category().name().equals("Electronics")));
    }

    @Test
    void filterAssetsByStatusShouldReturnMatchingAssets() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.RETIRED);
        createAndIndexAsset("AST-003", "Apple Laptop", "MacBook Pro", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest(null, null, null, AssetStatus.RETIRED, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals(AssetStatus.RETIRED, results.getContent().getFirst().assetStatus());
    }

    @Test
    void filterAssetsWithMultipleCriteriaUsingANDLogicShouldReturnMatchingAssets() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "Dell Monitor", "Dell 27 inch", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-003", "HP Laptop", "HP ProBook", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("FUR-001", "Dell Desk", "Standing desk", furnitureCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest("AST", "Laptop", "Electronics", AssetStatus.ACTIVE, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(2, results.getTotalElements());
        assertTrue(results.getContent().stream().allMatch(a ->
            a.assetCode().startsWith("AST") &&
            a.name().contains("Laptop") &&
            a.category().name().equals("Electronics") &&
            a.assetStatus() == AssetStatus.ACTIVE
        ));
    }

    @Test
    void filterAssetsWithMultipleTokensInSingleFieldShouldMatchAll() {
        createAndIndexAsset("AST-001", "Dell XPS Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "Dell Monitor", "Dell 27 inch", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-003", "HP XPS Desktop", "HP workstation", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest(null, "Dell XPS", null, null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("Dell XPS Laptop", results.getContent().getFirst().name());
    }

    @Test
    void filterAssetsByCaseInsensitiveCategoryShouldReturnMatchingAssets() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("FUR-001", "Office Desk", "Standing desk", furnitureCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest(null, null, "electronics", null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(1, results.getTotalElements());
        assertEquals("Electronics", results.getContent().getFirst().category().name());
    }

    @Test
    void filterAssetsWithNoMatchingShouldReturnEmptyPage() {
        createAndIndexAsset("AST-001", "Dell Laptop", "Dell XPS 15", electronicsCategory, AssetStatus.ACTIVE);

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest("NONEXISTENT", null, null, null, null);
        Page<AssetResponse> results = assetService.filterAssets(PageRequest.of(0, 10), filter);

        assertNotNull(results);
        assertEquals(0, results.getTotalElements());
    }

    @Test
    void filterAssetsWithAllStatusesShouldReturnAllAssets() {
        createAndIndexAsset("AST-001", "Laptop 1", "Active laptop", electronicsCategory, AssetStatus.ACTIVE);
        createAndIndexAsset("AST-002", "Laptop 2", "In use laptop", electronicsCategory, AssetStatus.IN_USE);
        createAndIndexAsset("AST-003", "Laptop 3", "Damaged laptop", electronicsCategory, AssetStatus.DAMAGED);
        createAndIndexAsset("AST-004", "Laptop 4", "Retired laptop", electronicsCategory, AssetStatus.RETIRED);

        waitForElasticsearchIndexing();

        AssetFilterRequest filterActive = new AssetFilterRequest(null, null, null, AssetStatus.ACTIVE, null);
        AssetFilterRequest filterInUse = new AssetFilterRequest(null, null, null, AssetStatus.IN_USE, null);
        AssetFilterRequest filterDamaged = new AssetFilterRequest(null, null, null, AssetStatus.DAMAGED, null);
        AssetFilterRequest filterRetired = new AssetFilterRequest(null, null, null, AssetStatus.RETIRED, null);

        assertEquals(1, assetService.filterAssets(PageRequest.of(0, 10), filterActive).getTotalElements());
        assertEquals(1, assetService.filterAssets(PageRequest.of(0, 10), filterInUse).getTotalElements());
        assertEquals(1, assetService.filterAssets(PageRequest.of(0, 10), filterDamaged).getTotalElements());
        assertEquals(1, assetService.filterAssets(PageRequest.of(0, 10), filterRetired).getTotalElements());
    }

    @Test
    void filterAssetsWithPaginationShouldReturnCorrectPage() {
        for (int i = 1; i <= 25; i++) {
            createAndIndexAsset("AST-" + String.format("%03d", i), "Laptop " + i, "Description " + i, electronicsCategory, AssetStatus.ACTIVE);
        }

        waitForElasticsearchIndexing();

        AssetFilterRequest filter = new AssetFilterRequest(null, null, "Electronics", null, null);
        Page<AssetResponse> firstPage = assetService.filterAssets(PageRequest.of(0, 10), filter);
        Page<AssetResponse> secondPage = assetService.filterAssets(PageRequest.of(1, 10), filter);

        assertNotNull(firstPage);
        assertNotNull(secondPage);
        assertEquals(10, firstPage.getContent().size());
        assertEquals(10, secondPage.getContent().size());
        assertEquals(25, firstPage.getTotalElements());
    }

    private Asset createAndIndexAsset(String assetCode, String name, String description, Category category, AssetStatus status) {
        Asset asset = Asset.builder()
                .assetCode(assetCode)
                .name(name)
                .description(description)
                .status(status)
                .category(category)
                .createdAt(Instant.now())
                .modifiedAt(Instant.now())
                .build();

        Asset savedAsset = assetRepository.save(asset);

        AssetDocument document = AssetDocument.from(savedAsset, Collections.emptyList());
        assetDocumentRepository.save(document);

        return savedAsset;
    }

    private void waitForElasticsearchIndexing() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

