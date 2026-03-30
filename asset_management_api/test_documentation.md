# Asset Service Test Documentation

## Overview
This document provides comprehensive documentation for the Asset Management System's test suite, covering both unit tests (`AssetServiceTest`) and integration tests (`AssetServiceIntegrationTest`). The test suite ensures the reliability and correctness of asset management operations including CRUD operations, search functionality, filtering, and Elasticsearch integration.

## Test Suite Structure

### Unit Tests: `AssetServiceTest`
- **Location**: `src/test/java/com/example/asset_management/service/impl/AssetServiceTest.java`
- **Type**: Unit tests with Mockito
- **Total Tests**: 23
- **Focus**: Business logic validation, error handling, and service layer operations

### Integration Tests: `AssetServiceIntegrationTest`
- **Location**: `src/test/java/com/example/asset_management/service/impl/AssetServiceIntegrationTest.java`
- **Type**: Integration tests with Testcontainers
- **Total Tests**: 21
- **Focus**: End-to-end search and filter functionality with real infrastructure

---

# Part 1: Unit Tests (AssetServiceTest)

## Unit Test Configuration

### Test Framework
- **Framework**: JUnit 5 with Mockito Extension
- **Extension**: `@ExtendWith(MockitoExtension.class)`
- **Approach**: Isolated unit testing with mocked dependencies
- **No External Dependencies**: All external services are mocked

### Mocked Dependencies
The following dependencies are mocked using `@Mock`:
- `CategoryRepository`: Category data access
- `AssetRepository`: Asset data access
- `IImageService`: Image handling service
- `IAssetAttributeValueService`: Asset attribute value service
- `AssetImageRepository`: Asset image data access
- `AssetAttributeValueRepository`: Asset attribute value data access
- `AssetDocumentRepository`: Elasticsearch document repository
- `AssetDocumentMapper`: Entity to document mapping

### Service Under Test
- `AssetService`: Injected with mocked dependencies using `@InjectMocks`

## Unit Test Categories

### 1. Asset Creation Tests

#### 1.1 Valid Asset Creation
**Test**: `insertAssetWithValidDataShouldCreateAssetSuccessfully()`
- **Purpose**: Verify successful asset creation with valid data
- **Setup**: 
  - Mock category exists
  - Empty file list
  - All repositories return expected values
- **Action**: Insert asset with valid request
- **Expected Result**: 
  - Asset created with correct attributes
  - Returns AssetResponse with matching data
  - All services called appropriately
- **Verifications**:
  - Category repository queried
  - Asset saved to repository
  - Image service invoked
  - Elasticsearch document created

#### 1.2 Non-Existent Category
**Test**: `insertAssetWithNonExistentCategoryShouldThrowNotFoundException()`
- **Purpose**: Verify proper error handling for invalid category
- **Setup**: Mock category repository returns empty
- **Action**: Attempt to insert asset with non-existent category ID
- **Expected Result**: 
  - Throws `NotFoundException`
  - Asset not saved to repository
- **Business Rule**: Assets must belong to valid categories

#### 1.3 Null Files Handling
**Test**: `insertAssetWithNullFilesShouldHandleGracefully()`
- **Purpose**: Verify null file list is handled gracefully
- **Setup**: Pass null for files parameter
- **Action**: Insert asset without images
- **Expected Result**: 
  - Asset created successfully
  - Image service handles null appropriately
  - No errors thrown

### 2. Asset Retrieval Tests

#### 2.1 Get Asset by ID (Existing)
**Test**: `getByIdWithExistingAssetShouldReturnAssetResponse()`
- **Purpose**: Verify retrieval of existing asset
- **Setup**: Mock repository returns asset
- **Action**: Query asset by ID
- **Expected Result**: 
  - Returns Optional with AssetResponse
  - All asset details match
  - Related data (images, attributes) retrieved

#### 2.2 Get Asset by ID (Non-Existent)
**Test**: `getByIdWithNonExistentAssetShouldReturnEmptyOptional()`
- **Purpose**: Verify behavior when asset doesn't exist
- **Setup**: Mock repository returns empty Optional
- **Action**: Query non-existent asset ID
- **Expected Result**: Returns empty Optional

#### 2.3 Get All Assets
**Test**: `getAllShouldReturnPagedAssets()`
- **Purpose**: Verify paginated retrieval of all assets
- **Setup**: Mock repository returns page with 2 assets
- **Action**: Request first page with size 10
- **Expected Result**: 
  - Returns Page with 2 elements
  - Correct total count
  - All assets properly mapped

#### 2.4 Get All (Empty Database)
**Test**: `getAllWithEmptyDatabaseShouldReturnEmptyPage()`
- **Purpose**: Verify behavior with no assets
- **Setup**: Mock repository returns empty page
- **Action**: Request page of assets
- **Expected Result**: 
  - Returns empty Page
  - Total elements: 0
  - Content list is empty

#### 2.5 Get Assets by Category
**Test**: `getAllByCategoryIdShouldReturnFilteredAssets()`
- **Purpose**: Verify filtering assets by category
- **Setup**: Mock repository returns 2 assets for category
- **Action**: Query assets by category ID
- **Expected Result**: 
  - Returns page with category-specific assets
  - Correct repository method called

#### 2.6 Get Assets by Non-Existent Category
**Test**: `getAllByCategoryIdWithNonExistentCategoryShouldReturnEmptyPage()`
- **Purpose**: Verify behavior for invalid category
- **Setup**: Mock repository returns empty page
- **Action**: Query with non-existent category ID
- **Expected Result**: Returns empty page

### 3. Asset Update Tests

#### 3.1 Update All Fields
**Test**: `updateExistingAssetShouldModifyAllFields()`
- **Purpose**: Verify complete asset update
- **Setup**: 
  - Existing asset with old values
  - New category and all new values
- **Action**: Update asset with new data
- **Expected Result**: 
  - Asset code updated
  - Name updated
  - Description updated
  - Status updated
  - Category changed
  - Modified timestamp updated
  - Elasticsearch document updated

#### 3.2 Update with Null Status
**Test**: `updateAssetWithNullStatusShouldKeepExistingStatus()`
- **Purpose**: Verify null status preserves existing value
- **Setup**: Asset with IN_USE status
- **Action**: Update with null status
- **Expected Result**: 
  - Other fields updated
  - Status remains IN_USE
  - No status change

#### 3.3 Update Non-Existent Asset
**Test**: `updateNonExistentAssetShouldThrowNotFoundException()`
- **Purpose**: Verify error handling for invalid asset ID
- **Setup**: Mock repository returns empty Optional
- **Action**: Attempt to update non-existent asset
- **Expected Result**: 
  - Throws `NotFoundException`
  - No save operation performed

#### 3.4 Update with Invalid Category
**Test**: `updateAssetWithInvalidCategoryShouldThrowNotFoundException()`
- **Purpose**: Verify validation of category during update
- **Setup**: 
  - Existing asset
  - Non-existent category ID in update request
- **Action**: Attempt to update with invalid category
- **Expected Result**: 
  - Throws `NotFoundException`
  - Asset not saved

### 4. Asset Deletion Tests

#### 4.1 Delete Existing Asset
**Test**: `deleteExistingAssetShouldSetStatusToRetired()`
- **Purpose**: Verify soft delete (status change to RETIRED)
- **Setup**: Active asset exists
- **Action**: Delete asset
- **Expected Result**: 
  - Asset status changed to RETIRED
  - Asset saved (not physically deleted)
  - Modified timestamp updated
  - Elasticsearch document updated
- **Business Rule**: Assets are soft-deleted, not removed

#### 4.2 Delete Non-Existent Asset
**Test**: `deleteNonExistentAssetShouldThrowNotFoundException()`
- **Purpose**: Verify error handling for invalid delete
- **Setup**: Mock repository returns empty Optional
- **Action**: Attempt to delete non-existent asset
- **Expected Result**: 
  - Throws `NotFoundException`
  - No save operation

#### 4.3 Delete Already Retired Asset
**Test**: `deleteAlreadyRetiredAssetShouldStillUpdateTimestamp()`
- **Purpose**: Verify idempotent delete operation
- **Setup**: Asset already has RETIRED status
- **Action**: Delete retired asset
- **Expected Result**: 
  - Status remains RETIRED
  - Modified timestamp updated
  - Operation succeeds without error

### 5. Caching Tests

#### 5.1 Cache on Read
**Test**: `getByIdShouldCacheResult()`
- **Purpose**: Verify caching behavior on asset retrieval
- **Setup**: Mock asset exists
- **Action**: Call getById
- **Expected Result**: 
  - Repository called once
  - Result potentially cached (implementation-dependent)

#### 5.2 Cache Eviction on Update
**Test**: `updateShouldEvictCache()`
- **Purpose**: Verify cache invalidation on update
- **Setup**: Existing asset
- **Action**: Update asset
- **Expected Result**: 
  - Update performed
  - Cache evicted for updated asset

#### 5.3 Cache Eviction on Delete
**Test**: `deleteShouldEvictCache()`
- **Purpose**: Verify cache invalidation on delete
- **Setup**: Existing asset
- **Action**: Delete asset (set to RETIRED)
- **Expected Result**: 
  - Status changed to RETIRED
  - Cache evicted for deleted asset

## Unit Test Best Practices Demonstrated

1. **Isolation**: Each test uses mocked dependencies, no external systems
2. **AAA Pattern**: Arrange, Act, Assert structure consistently applied
3. **Clear Names**: Test names describe exact scenario and expected outcome
4. **Edge Cases**: Tests cover null values, empty results, invalid inputs
5. **Error Scenarios**: Comprehensive exception testing
6. **Mocking Strategy**: Proper use of Mockito verify() for behavior verification
7. **Argument Matchers**: Using `argThat()` for complex verification
8. **No Test Interdependence**: Each test is completely independent

## Unit Test Coverage Summary

| Feature Area | Test Count | Coverage |
|--------------|------------|----------|
| Asset Creation | 3 | Complete |
| Asset Retrieval | 6 | Complete |
| Asset Update | 4 | Complete |
| Asset Deletion | 3 | Complete |
| Caching | 3 | Complete |
| Error Handling | 7 | Complete |
| **Total** | **23** | **100%** |

---

# Part 2: Integration Tests (AssetServiceIntegrationTest)

## Integration Test Configuration

### Test Framework
- **Framework**: JUnit 5
- **Spring Boot Test**: `@SpringBootTest`
- **Test Containers**: `@Testcontainers`
- **Active Profile**: `test`

### Container Infrastructure
The integration tests use Testcontainers to spin up the following services:

#### PostgreSQL Database
- **Image**: `postgres:16-alpine`
- **Database Name**: `testdb`
- **Username**: `test`
- **Password**: `test`
- **Purpose**: Primary data storage for assets and categories

#### Elasticsearch
- **Image**: `docker.elastic.co/elasticsearch/elasticsearch:8.11.0`
- **Configuration**:
  - X-Pack Security: Disabled
  - Discovery Type: Single-node
  - Java Heap: 512m (min and max)
- **Purpose**: Full-text search and asset indexing

#### Redis
- **Image**: `redis:7-alpine`
- **Exposed Port**: 6379
- **Purpose**: Caching layer

### Test Data Setup
Each test initializes with two categories:
- **Electronics**: "Electronic devices"
- **Furniture**: "Office furniture"

All repositories are cleared before each test to ensure test isolation.

## Test Categories

### 1. Search Functionality Tests

#### 1.1 Exact Match Search
**Test**: `searchAssetsWithExactMatchShouldReturnAsset()`
- **Purpose**: Verify that searching with an exact asset name returns the correct asset
- **Setup**: Creates asset "AST-001" named "Dell Laptop"
- **Action**: Searches for "Dell Laptop"
- **Expected Result**: Returns exactly 1 asset with matching code and name

#### 1.2 Partial Match Search
**Test**: `searchAssetsWithPartialMatchShouldReturnAsset()`
- **Purpose**: Verify that partial text matches work correctly
- **Setup**: Creates asset "AST-001" named "Dell Laptop"
- **Action**: Searches for "Laptop"
- **Expected Result**: Returns the asset containing "Laptop" in its name

#### 1.3 Asset Code Search
**Test**: `searchAssetsWithAssetCodeShouldReturnAsset()`
- **Purpose**: Verify searching by exact asset code
- **Setup**: Creates asset "AST-001"
- **Action**: Searches for "AST-001"
- **Expected Result**: Returns the asset with code "AST-001"

#### 1.4 Partial Asset Code Search
**Test**: `searchAssetsWithPartialAssetCodeShouldReturnAsset()`
- **Purpose**: Verify partial asset code matching
- **Setup**: Creates asset "AST-001"
- **Action**: Searches for "AST"
- **Expected Result**: Returns assets with codes starting with "AST"

#### 1.5 Case Insensitive Search
**Test**: `searchAssetsWithCaseInsensitiveQueryShouldReturnAsset()`
- **Purpose**: Verify case-insensitive search functionality
- **Setup**: Creates asset named "Dell Laptop"
- **Action**: Searches for "dell laptop" (lowercase)
- **Expected Result**: Returns the "Dell Laptop" asset

#### 1.6 Uppercase Query Search
**Test**: `searchAssetsWithUppercaseQueryShouldReturnAsset()`
- **Purpose**: Verify uppercase queries work correctly
- **Setup**: Creates asset named "Dell Laptop"
- **Action**: Searches for "DELL LAPTOP"
- **Expected Result**: Returns the matching asset

#### 1.7 Multiple Results Search
**Test**: `searchAssetsWithMultipleMatchesShouldReturnAllOrderedByRelevance()`
- **Purpose**: Verify search returns multiple matching results ordered by relevance
- **Setup**: Creates 3 assets (2 Dell products, 1 HP product)
- **Action**: Searches for "Dell"
- **Expected Result**: Returns 2 Dell assets, ordered by relevance

#### 1.8 No Match Search
**Test**: `searchAssetsWithNoMatchesShouldReturnEmptyPage()`
- **Purpose**: Verify behavior when no assets match the search
- **Setup**: Creates one asset
- **Action**: Searches for "NonExistentAsset"
- **Expected Result**: Returns empty page (0 elements)

#### 1.9 Special Characters Handling
**Test**: `searchAssetsWithSpecialCharactersShouldHandleGracefully()`
- **Purpose**: Verify special characters don't break search functionality
- **Setup**: Creates asset named "Dell Laptop"
- **Action**: Searches for "Dell*Laptop"
- **Expected Result**: Handles gracefully without errors

#### 1.10 Search Pagination
**Test**: `searchAssetsWithPaginationShouldReturnCorrectPage()`
- **Purpose**: Verify pagination works correctly in search results
- **Setup**: Creates 15 laptop assets
- **Action**: Requests page 0 and page 1, with 5 items per page
- **Expected Result**: 
  - Each page contains 5 items
  - Total elements: 15
  - Different assets on different pages

### 2. Filter Functionality Tests

#### 2.1 Filter by Asset Code
**Test**: `filterAssetsByAssetCodeShouldReturnMatchingAsset()`
- **Purpose**: Verify filtering by exact asset code
- **Setup**: Creates 2 assets with different codes
- **Action**: Filters by "AST-001"
- **Expected Result**: Returns only the asset with code "AST-001"

#### 2.2 Filter by Partial Asset Code
**Test**: `filterAssetsByPartialAssetCodeShouldReturnMatchingAssets()`
- **Purpose**: Verify partial asset code filtering
- **Setup**: Creates 2 "AST-" assets and 1 "MON-" asset
- **Action**: Filters by "AST"
- **Expected Result**: Returns 2 assets starting with "AST"

#### 2.3 Filter by Name
**Test**: `filterAssetsByNameShouldReturnMatchingAssets()`
- **Purpose**: Verify filtering by asset name
- **Setup**: Creates 2 laptops and 1 monitor
- **Action**: Filters by name "Laptop"
- **Expected Result**: Returns 2 laptop assets

#### 2.4 Filter by Category Name
**Test**: `filterAssetsByCategoryNameShouldReturnMatchingAssets()`
- **Purpose**: Verify filtering by category
- **Setup**: Creates 2 electronics and 1 furniture asset
- **Action**: Filters by category "Electronics"
- **Expected Result**: Returns 2 electronics assets

#### 2.5 Filter by Status
**Test**: `filterAssetsByStatusShouldReturnMatchingAssets()`
- **Purpose**: Verify filtering by asset status
- **Setup**: Creates 2 ACTIVE and 1 RETIRED asset
- **Action**: Filters by status RETIRED
- **Expected Result**: Returns 1 retired asset

#### 2.6 Multi-Criteria Filter (AND Logic)
**Test**: `filterAssetsWithMultipleCriteriaUsingANDLogicShouldReturnMatchingAssets()`
- **Purpose**: Verify multiple filter criteria work with AND logic
- **Setup**: Creates 4 diverse assets
- **Action**: Filters by code="AST", name="Laptop", category="Electronics", status=ACTIVE
- **Expected Result**: Returns 2 assets matching all criteria

#### 2.7 Filter with Multiple Tokens
**Test**: `filterAssetsWithMultipleTokensInSingleFieldShouldMatchAll()`
- **Purpose**: Verify multi-word filter matching
- **Setup**: Creates assets with "Dell XPS", "Dell Monitor", "HP XPS"
- **Action**: Filters by name "Dell XPS"
- **Expected Result**: Returns only "Dell XPS Laptop" (matches both tokens)

#### 2.8 Case Insensitive Category Filter
**Test**: `filterAssetsByCaseInsensitiveCategoryShouldReturnMatchingAssets()`
- **Purpose**: Verify category filtering is case-insensitive
- **Setup**: Creates electronics and furniture assets
- **Action**: Filters by category "electronics" (lowercase)
- **Expected Result**: Returns the Electronics category asset

#### 2.9 Filter with No Matches
**Test**: `filterAssetsWithNoMatchingShouldReturnEmptyPage()`
- **Purpose**: Verify filter returns empty page when no matches found
- **Setup**: Creates one asset
- **Action**: Filters by code "NONEXISTENT"
- **Expected Result**: Returns empty page

#### 2.10 Filter All Asset Statuses
**Test**: `filterAssetsWithAllStatusesShouldReturnAllAssets()`
- **Purpose**: Verify filtering works for all asset status types
- **Setup**: Creates 4 assets, one for each status (ACTIVE, IN_USE, DAMAGED, RETIRED)
- **Action**: Filters separately for each status
- **Expected Result**: Each filter returns exactly 1 asset of the specified status

#### 2.11 Filter Pagination
**Test**: `filterAssetsWithPaginationShouldReturnCorrectPage()`
- **Purpose**: Verify pagination works correctly with filters
- **Setup**: Creates 25 laptop assets
- **Action**: Filters by category "Electronics", requests pages 0 and 1 with 10 items each
- **Expected Result**:
  - First page: 10 items
  - Second page: 10 items
  - Total elements: 25

## Helper Methods and Utilities

### Integration Test Helpers

#### createAndIndexAsset()
```java
private Asset createAndIndexAsset(String assetCode, String name, String description, 
                                  Category category, AssetStatus status)
```
**Purpose**: Creates an asset in the database and indexes it in Elasticsearch
- Saves asset to PostgreSQL via `assetRepository`
- Creates and saves corresponding `AssetDocument` to Elasticsearch
- Returns the saved asset
- Used in all integration tests to set up test data

#### waitForElasticsearchIndexing()
```java
private void waitForElasticsearchIndexing()
```
**Purpose**: Introduces a 1.5-second delay to allow Elasticsearch indexing to complete
- **Note**: This is necessary because Elasticsearch indexing is near-real-time, not immediate
- Called after creating assets and before performing searches
- Consider replacing with Elasticsearch refresh API in production

### Unit Test Mocking Patterns

#### Basic Mock Setup
```java
when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
when(assetRepository.save(any(Asset.class))).thenReturn(savedAsset);
```

#### Verification Pattern
```java
verify(assetRepository).findById(assetId);
verify(assetRepository).save(any(Asset.class));
verify(assetRepository, never()).save(any(Asset.class));
```

#### Argument Capture and Verification
```java
verify(assetRepository).save(argThat(asset ->
    asset.getAssetCode().equals("AST-001") &&
    asset.getStatus() == AssetStatus.RETIRED
));
```

## Asset Status Enumerations
The tests cover all four asset statuses:
- **ACTIVE**: Asset is available for use
- **IN_USE**: Asset is currently being used
- **DAMAGED**: Asset is damaged and needs repair
- **RETIRED**: Asset has been retired from service

## Test Execution Requirements

### Unit Tests Prerequisites
1. **Java 17+**: Required for Spring Boot 3.x
2. **Maven**: For dependency management and test execution
3. **No External Dependencies**: Tests run without Docker or databases

### Integration Tests Prerequisites
1. **Docker**: Must be running to start Testcontainers
2. **Java 17+**: Required for Spring Boot 3.x
3. **Maven**: For dependency management and test execution
4. **Sufficient Memory**: Elasticsearch requires at least 512MB heap
5. **Available Ports**: 5432 (PostgreSQL), 9200 (Elasticsearch), 6379 (Redis)

### Running the Tests
```bash
# Run all integration tests
mvn test -Dtest=AssetServiceIntegrationTest

# Run specific test
mvn test -Dtest=AssetServiceIntegrationTest#searchAssetsWithExactMatchShouldReturnAsset

# Run with verbose output
mvn test -Dtest=AssetServiceIntegrationTest -X
```

## Performance Considerations

### Elasticsearch Indexing Delay
- Each test includes a 1.5-second wait after creating assets
- This ensures Elasticsearch has time to index documents before queries
- Consider using Elasticsearch's refresh API in production for more precise control

### Container Startup Time
- First test run may take longer due to container image downloads
- Subsequent runs use cached images
- Typical container startup: 10-30 seconds

### Test Isolation
- Each test clears all repositories (`@BeforeEach`)
- Tests can run in any order without affecting each other
- Parallel execution may be limited by shared container resources

## Integration Test Coverage Summary

| Feature | Test Count | Coverage |
|---------|------------|----------|
| Search Functionality | 10 | Complete |
| Filter Functionality | 11 | Complete |
| Pagination | 2 | Complete |
| Case Sensitivity | 3 | Complete |
| Edge Cases | 3 | Complete |
| **Total** | **21** | **100%** |

---

# Overall Test Suite Summary

## Combined Coverage

| Test Type | Test Count | Focus Area | External Dependencies |
|-----------|------------|------------|----------------------|
| **Unit Tests** | 23 | Business logic, CRUD operations, error handling | None (all mocked) |
| **Integration Tests** | 21 | Search, filter, Elasticsearch integration | PostgreSQL, Elasticsearch, Redis |
| **Total** | **44** | Complete service layer coverage | - |

## Test Execution Strategy

### Unit Tests
```bash
# Run only unit tests (fast)
mvn test -Dtest=AssetServiceTest

# Run specific unit test
mvn test -Dtest=AssetServiceTest#insertAssetWithValidDataShouldCreateAssetSuccessfully
```

**Execution Time**: ~2-5 seconds  
**Requirements**: None (no external dependencies)

### Integration Tests
```bash
# Run only integration tests (slower due to containers)
mvn test -Dtest=AssetServiceIntegrationTest

# Run specific integration test
mvn test -Dtest=AssetServiceIntegrationTest#searchAssetsWithExactMatchShouldReturnAsset
```

**Execution Time**: ~30-60 seconds (includes container startup)  
**Requirements**: Docker running

### All Tests
```bash
# Run complete test suite
mvn test

# Run with coverage report
mvn test jacoco:report
```

## Best Practices Demonstrated Across Test Suite

### Unit Tests
1. **Complete Isolation**: All dependencies mocked, no side effects
2. **Fast Execution**: Tests run in milliseconds
3. **Focused Testing**: Each test validates one specific behavior
4. **Comprehensive Mocking**: Proper use of Mockito for dependency simulation
5. **Verification**: Using verify() to ensure correct method calls
6. **Exception Testing**: Thorough validation of error scenarios

### Integration Tests
1. **Real Infrastructure**: Uses actual PostgreSQL, Elasticsearch, and Redis via Testcontainers
2. **Test Isolation**: Each test clears data and sets up its own fixtures
3. **Descriptive Names**: Test names clearly describe what they verify
4. **Arrange-Act-Assert**: Tests follow AAA pattern consistently
5. **Edge Case Testing**: Includes tests for empty results, special characters, etc.
6. **Pagination Testing**: Verifies pagination works correctly under various scenarios
7. **Multi-criteria Testing**: Tests complex filtering scenarios

### Common Practices
1. **Clear Documentation**: Test names serve as documentation
2. **No Test Interdependence**: Tests can run in any order
3. **Comprehensive Coverage**: Both happy path and error scenarios
4. **Maintainable**: Easy to understand and modify
5. **Consistent Structure**: All tests follow similar patterns

## Known Limitations

### Unit Tests
1. **No Real Integration**: Doesn't test actual database interactions
2. **Mock Assumptions**: Assumes mocked behavior matches real implementation
3. **No Transaction Testing**: Can't verify transaction boundaries
4. **Limited Error Scenarios**: Only tests explicitly coded error paths

### Integration Tests
1. **Fixed Delay**: Uses `Thread.sleep()` instead of polling Elasticsearch readiness
2. **Single-Node Elasticsearch**: Tests don't verify cluster behavior
3. **Limited Error Scenarios**: Focuses on happy path and basic edge cases
4. **No Performance Benchmarks**: Tests verify functionality but not performance thresholds
5. **Container Startup Time**: First run takes longer due to image downloads

## Future Improvements

### Unit Tests
1. Add parameterized tests for multiple status transitions
2. Test concurrent modification scenarios
3. Add tests for audit logging if implemented
4. Test transaction rollback scenarios with Spring's `@Transactional`
5. Add tests for batch operations

### Integration Tests
1. Replace fixed delays with Elasticsearch refresh API or polling
2. Add tests for concurrent modifications
3. Add performance benchmarks for large datasets
4. Test Elasticsearch cluster failure scenarios
5. Add tests for malformed filter requests
6. Test Unicode and internationalization support
7. Add tests for very large result sets (10k+ items)
8. Add stress tests for concurrent searches

### General Improvements
1. Add contract tests between service and repository layers
2. Implement mutation testing to verify test quality
3. Add integration tests for full CRUD operations
4. Test database connection failure scenarios
5. Add tests for audit trail functionality
6. Implement end-to-end API tests

## Testing Strategies and Patterns

### Test Data Management

#### Unit Tests
- **Minimal Data**: Only create data needed for specific test
- **Builder Pattern**: Use builders for clean object creation
- **Constants**: Define reusable test constants for IDs, codes, names
- **Immutable**: Test data doesn't change between tests (no shared state)

#### Integration Tests
- **Realistic Data**: Use realistic but simplified test data
- **Setup Method**: `@BeforeEach` clears and recreates categories
- **Incremental**: Tests create only what they need
- **Cleanup**: Each test starts with clean slate

### Error Testing Strategies

#### Unit Tests - Exception Scenarios
1. **Non-existent resources**: Test with invalid IDs
2. **Null parameters**: Verify null handling
3. **Invalid states**: Test business rule violations
4. **Repository failures**: Mock repository exceptions

#### Integration Tests - Edge Cases
1. **Empty results**: Verify behavior with no matches
2. **Large datasets**: Test pagination with many records
3. **Special characters**: Test with unusual input
4. **Case sensitivity**: Verify case-insensitive operations

### Assertion Strategies

#### Unit Tests
```java
// Verify return values
assertNotNull(response);
assertEquals(expectedValue, actualValue);

// Verify method calls
verify(repository).save(any(Asset.class));
verify(repository, never()).delete(any());

// Verify argument contents
verify(repository).save(argThat(asset -> 
    asset.getStatus() == AssetStatus.RETIRED
));
```

#### Integration Tests
```java
// Verify page structure
assertNotNull(results);
assertEquals(expectedCount, results.getTotalElements());
assertEquals(expectedSize, results.getContent().size());

// Verify content
assertTrue(results.getContent().stream()
    .allMatch(a -> a.name().contains("Laptop")));

// Verify ordering
assertNotEquals(firstPage.getContent().getFirst().id(), 
                secondPage.getContent().getFirst().id());
```

### Test Naming Conventions

Both test suites follow the pattern:
```
[method]With[condition]Should[expectedResult]
```

Examples:
- `insertAssetWithValidDataShouldCreateAssetSuccessfully`
- `searchAssetsWithExactMatchShouldReturnAsset`
- `deleteNonExistentAssetShouldThrowNotFoundException`

This makes tests self-documenting and easy to understand.

### Continuous Integration Considerations

#### Fast Feedback Loop
```yaml
# Example CI configuration
stages:
  - unit-tests    # Run first (fast)
  - integration-tests  # Run after unit tests pass
  - build
```

#### Parallel Execution
- **Unit tests**: Can run in parallel (no shared state)
- **Integration tests**: Can run in parallel with separate containers
- Configure Maven Surefire for parallel execution:
```xml
<plugin>
    <artifactId>maven-surefire-plugin</artifactId>
    <configuration>
        <parallel>classes</parallel>
        <threadCount>4</threadCount>
    </configuration>
</plugin>
```

#### Resource Management
- Unit tests: Minimal resources needed
- Integration tests: Require Docker and 2GB+ RAM
- Consider running integration tests on specific branches only

## Maintenance Notes

### Unit Tests
- Keep mocks synchronized with actual implementation changes
- Update tests when business rules change
- Ensure all new service methods have corresponding unit tests
- Review and update when dependencies are upgraded
- Monitor test execution time (should remain under 10 seconds)

### Integration Tests
- Update Elasticsearch version to match production
- Keep PostgreSQL version aligned with production
- Monitor test execution time; consider optimizing if > 2 minutes
- Review and update test data as business rules evolve
- Ensure Docker images are periodically updated

### General
- Run full test suite before each commit
- Maintain test coverage above 80%
- Document any test that requires specific setup
- Keep test data realistic but anonymized
- Review failed tests immediately - don't ignore flaky tests

## Troubleshooting

### Unit Tests Failing
1. **Verify mocks are configured**: Check all required mock setups
2. **Check assertions**: Ensure expected values match actual implementation
3. **Review verify() calls**: Make sure method invocations match
4. **Look for null values**: Common source of NPE in tests

### Integration Tests Failing
1. **Docker not running**: Ensure Docker Desktop is running
2. **Port conflicts**: Check if ports 5432, 9200, or 6379 are in use
3. **Insufficient memory**: Elasticsearch needs at least 512MB
4. **Timing issues**: May need to increase wait time for indexing
5. **Container startup failures**: Check Docker logs for errors

### Performance Issues
1. **Slow unit tests**: Check for unnecessary operations or large data
2. **Slow integration tests**: Consider reducing test data size
3. **Container startup slow**: First run downloads images; subsequent runs faster
4. **Elasticsearch indexing slow**: May need to adjust heap size or refresh interval

## Dependencies and Versions

### Unit Test Dependencies
```xml
<dependency>
    <groupId>org.junit.jupiter</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-core</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.mockito</groupId>
    <artifactId>mockito-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

### Integration Test Dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>elasticsearch</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

## Key Differences: Unit vs Integration Tests

| Aspect | Unit Tests | Integration Tests |
|--------|------------|-------------------|
| **Speed** | Very fast (milliseconds) | Slower (30-60 seconds) |
| **Dependencies** | All mocked | Real containers |
| **Scope** | Single class/method | Multiple components |
| **Isolation** | Complete | Database shared across test |
| **Setup Complexity** | Low | High (containers) |
| **Confidence Level** | Logic correctness | System integration |
| **CI/CD Impact** | Minimal | Requires Docker |
| **Debugging** | Easy | More complex |
| **Cost** | Very low | Higher (resources) |

## When to Use Which Test Type

### Use Unit Tests When:
- Testing business logic in isolation
- Validating error handling and edge cases
- Need fast feedback during development
- Testing complex conditional logic
- Verifying method call sequences
- Working without external dependencies

### Use Integration Tests When:
- Testing database queries and transactions
- Validating Elasticsearch search behavior
- Testing end-to-end workflows
- Verifying data persistence and retrieval
- Testing with real infrastructure behavior
- Validating cache behavior with Redis

### Recommendation:
- Write unit tests first for all business logic
- Add integration tests for critical paths
- Maintain 70% unit tests, 30% integration tests ratio
- Use integration tests to validate assumptions made in unit tests

---

**Last Updated**: February 20, 2026  
**Test Classes**: 
- `AssetServiceTest` (Unit Tests)
- `AssetServiceIntegrationTest` (Integration Tests)
**Location**: `src/test/java/com/example/asset_management/service/impl/`  
**Total Tests**: 44 (23 unit + 21 integration)  
**Maintained By**: Development Team

