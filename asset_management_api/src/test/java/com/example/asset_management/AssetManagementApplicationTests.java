package com.example.asset_management;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
		"server.ssl.enabled=false",
		"spring.liquibase.enabled=false",
		"spring.elasticsearch.uris=http://localhost:9200",
		"spring.elasticsearch.password=test",
		"spring.datasource.url=jdbc:postgresql://localhost:5432/testdb",
		"spring.datasource.username=test",
		"spring.datasource.password=test",
		"authorization.token.secret=test-secret-key-for-testing-purposes-only",
		"authorization.token.refresh-secret=test-refresh-secret-key-for-testing-purposes-only",
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379"
})
class AssetManagementApplicationTests {

	@Test
	void contextLoads() {
	}

}
