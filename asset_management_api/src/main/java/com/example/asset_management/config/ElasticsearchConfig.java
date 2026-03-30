package com.example.asset_management.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.ssl.SSLContextBuilder;
import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.net.ssl.SSLContext;
import java.net.URI;

@Configuration
@ConditionalOnProperty(name = "elasticsearch.custom.config.enabled", havingValue = "true", matchIfMissing = true)
public class ElasticsearchConfig {

    @Value("${spring.elasticsearch.uris:https://localhost:9200}")
    private String elasticsearchUri;

    @Value("${spring.elasticsearch.username:elastic}")
    private String elasticUsername;

    @Value("${spring.elasticsearch.password:}")
    private String elasticPassword;

    @Bean
    public ElasticsearchClient elasticsearchClient() {
        try {
            String uriString = elasticsearchUri;
            if (uriString.startsWith("[")) {
                uriString = uriString.substring(1, uriString.indexOf("]"));
            }

            URI uri = new URI(uriString);
            String scheme = uri.getScheme();
            String host = uri.getHost();
            int port = uri.getPort();

            org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(
                    new HttpHost(host, port, scheme)
            );

            if (elasticPassword != null && !elasticPassword.isEmpty()) {
                if ("https".equalsIgnoreCase(scheme)) {
                    SSLContext sslContext = SSLContextBuilder
                            .create()
                            .loadTrustMaterial(null, (chain, authType) -> true)
                            .build();

                    builder.setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder
                                    .setSSLContext(sslContext)
                                    .setDefaultCredentialsProvider(credentialsProvider())
                    );
                } else {
                    builder.setHttpClientConfigCallback(httpClientBuilder ->
                            httpClientBuilder
                                    .setDefaultCredentialsProvider(credentialsProvider())
                    );
                }
            }

            RestClient restClient = builder.build();
            ElasticsearchTransport transport =
                    new RestClientTransport(restClient, new JacksonJsonpMapper());

            return new ElasticsearchClient(transport);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Elasticsearch client", e);
        }
    }

    private CredentialsProvider credentialsProvider() {
        BasicCredentialsProvider provider = new BasicCredentialsProvider();
        provider.setCredentials(
                AuthScope.ANY,
                new UsernamePasswordCredentials(elasticUsername, elasticPassword)
        );
        return provider;
    }
}