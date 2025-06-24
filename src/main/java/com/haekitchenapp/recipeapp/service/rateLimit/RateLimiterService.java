package com.haekitchenapp.recipeapp.service.rateLimit;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Deque;
import java.util.concurrent.*;

@Service
public class RateLimiterService {

    private static final int MAX_PER_MINUTE = 5;
    private static final int MAX_PER_HOUR = 30;

    private static final long ONE_MINUTE = 60;
    private static final long ONE_HOUR = 3600;

    private final ConcurrentMap<String, Deque<Long>> userRequests = new ConcurrentHashMap<>();

    public boolean isAllowed(String userKey) {
        long now = Instant.now().getEpochSecond();
        Deque<Long> timestamps = userRequests.computeIfAbsent(userKey, k -> new ConcurrentLinkedDeque<>());

        synchronized (timestamps) {
            // Remove expired entries
            timestamps.removeIf(ts -> now - ts > ONE_HOUR);

            // Count requests in last minute & hour
            long countLastMinute = timestamps.stream().filter(ts -> now - ts <= ONE_MINUTE).count();
            long countLastHour = timestamps.size();

            if (countLastMinute >= MAX_PER_MINUTE || countLastHour >= MAX_PER_HOUR) {
                return false;
            }

            // Record this request
            timestamps.addLast(now);
            return true;
        }
    }
}
