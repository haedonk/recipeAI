package com.haekitchenapp.recipeapp.service.rateLimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.*;

@Service
public class RateLimiterService {

    private static final Logger logger = LoggerFactory.getLogger(RateLimiterService.class);

    @Value("${rate-limiter.max-per-minute:30}")
    private int maxPerMinute;

    @Value("${rate-limiter.max-per-hour:100}")
    private int maxPerHour;

    // Use TimeUnit constants instead of configuration values
    private static final long ONE_MINUTE = TimeUnit.MINUTES.toSeconds(1);
    private static final long ONE_HOUR = TimeUnit.HOURS.toSeconds(1);

    private final ConcurrentMap<String, Deque<Long>> userRequests = new ConcurrentHashMap<>();

    public boolean isAllowed(String userKey, int maxPerMinute, int maxPerHour) {
        long now = Instant.now().getEpochSecond();
        Deque<Long> timestamps = userRequests.computeIfAbsent(userKey, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            timestamps.removeIf(ts -> now - ts > ONE_HOUR);

            long countLastMinute = timestamps.stream().filter(ts -> now - ts <= ONE_MINUTE).count();
            long countLastHour = timestamps.size();

            if (countLastMinute >= maxPerMinute || countLastHour >= maxPerHour) {
                return false;
            }

            timestamps.addLast(now);
            return true;
        }
    }

    /**
     * Decreases the rate limit count for a user to allow them to make a few more requests
     * Particularly useful to ensure users can still authenticate after hitting rate limits
     * @param userKey Identifier for the user (typically IP address)
     * @param requestsToReduce Number of requests to remove from their count
     */
    public void decreaseRateCount(String userKey, int requestsToReduce) {
        Deque<Long> timestamps = userRequests.get(userKey);
        if (timestamps != null) {
            synchronized (timestamps) {
                // Remove some recent requests to give the user a bit of breathing room
                // But don't completely reset their counter
                int removed = 0;
                while (!timestamps.isEmpty() && removed < requestsToReduce) {
                    timestamps.pollLast(); // Remove the most recent request
                    removed++;
                }
                logger.debug("Decreased rate count for {} by {} requests", userKey, removed);
            }
        }
    }
}
