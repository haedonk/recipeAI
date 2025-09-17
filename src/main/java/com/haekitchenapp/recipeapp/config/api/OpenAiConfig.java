package com.haekitchenapp.recipeapp.config.api;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "openai")
public class OpenAiConfig {

    private String apiKey;
    private String baseUrl;
    private String chatEndpoint;
    private String chatModel;
    private String embedEndpoint;
    private String embedModel;
    private String chatSmallModel;

    @Data
    public static class Client {
        private int maxConnections = 15;
        private int maxIdleTimeSeconds = 60;
        private int pendingAcquireTimeoutSeconds = 10;
        private int pendingAcquireMaxCount = 50;
        private int evictBackgroundSeconds = 30;
        private boolean enableMetrics = true;
        private int responseTimeoutSeconds = 60;
        private int connectTimeoutMillis = 10000;
        private int readTimeoutSeconds = 60;
        private int writeTimeoutSeconds = 60;
    }

    private Client client = new Client();
}
