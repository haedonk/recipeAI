package com.haekitchenapp.recipeapp.client;

import com.haekitchenapp.recipeapp.config.api.TogetherAiConfig;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Autowired
    private TogetherAiConfig config;

    @Bean
    public WebClient togetherWebClient() {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("together-ai-pool")
                .maxConnections(config.getClient().getMaxConnections())
                .maxIdleTime(Duration.ofSeconds(config.getClient().getMaxIdleTimeSeconds()))
                .pendingAcquireTimeout(Duration.ofSeconds(config.getClient().getPendingAcquireTimeoutSeconds()))
                .pendingAcquireMaxCount(config.getClient().getPendingAcquireMaxCount())
                .evictInBackground(Duration.ofSeconds(config.getClient().getEvictBackgroundSeconds()))
                .metrics(config.getClient().isEnableMetrics())
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .responseTimeout(Duration.ofSeconds(config.getClient().getResponseTimeoutSeconds()))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, config.getClient().getConnectTimeoutMillis())
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(config.getClient().getReadTimeoutSeconds()))
                                .addHandlerLast(new WriteTimeoutHandler(config.getClient().getWriteTimeoutSeconds()))
                );

        return WebClient.builder()
                .baseUrl(config.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + config.getApiKey())
                .defaultHeader("Content-Type", "application/json")
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}
