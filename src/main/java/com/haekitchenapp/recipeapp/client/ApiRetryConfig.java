package com.haekitchenapp.recipeapp.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.function.Supplier;

@Configuration
@Slf4j
public class ApiRetryConfig {

    @Value("${api.retry.max-retries:3}")
    private int maxRetries;
    @Value("${api.retry.delay-millis:1000}")
    private long delayMillis;

    public <T> T retryTemplate(Supplier<T> action){
        int attempts = 0;
        while (true) {
            try {
                log.info("Attempt {} of {}", attempts + 1, maxRetries);
                return action.get();
            } catch (Exception e) {
                log.error("Attempt {} failed: {}", attempts + 1, e.getMessage());
                if (++attempts >= maxRetries) throw e;
                try {
                    Thread.sleep(delayMillis);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Thread interrupted during retry delay", e);
                }
            }
        }
    }
}
