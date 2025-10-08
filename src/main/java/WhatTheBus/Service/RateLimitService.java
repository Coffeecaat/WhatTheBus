package WhatTheBus.Service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final int MAX_REQUESTS = 100;
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(1);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";

    private final StringRedisTemplate stringRedisTemplate;
    private final CircuitBreaker redisCircuitBreaker;

    public RateLimitService(StringRedisTemplate stringRedisTemplate,
                            @Qualifier("redisRateLimiterCircuitBreaker") CircuitBreaker redisCircuitBreaker) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisCircuitBreaker = redisCircuitBreaker;
    }

    public boolean isAllowed(String hashedClientId) {
        if (hashedClientId == null || hashedClientId.trim().isEmpty()) {
            return false;
        }

        Supplier<Boolean> rateLimitCall = () -> evaluateRateLimit(hashedClientId);

        try {
            return redisCircuitBreaker.executeSupplier(rateLimitCall);
        } catch (CallNotPermittedException ex) {
            log.warn("Redis rate-limiter circuit open; allowing request for client {}", hashedClientId);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Redis rate-limiter failed for client {} - allowing request", hashedClientId, ex);
            return true;
        }
    }

    private boolean evaluateRateLimit(String hashedClientId) {
        String key = RATE_LIMIT_KEY_PREFIX + hashedClientId;
        long currentTime = Instant.now().getEpochSecond();
        long windowStart = currentTime - WINDOW_SIZE.getSeconds();

        ZSetOperations<String, String> zSetOps = stringRedisTemplate.opsForZSet();
        Objects.requireNonNull(zSetOps, "Redis ZSet operations must not be null");

        zSetOps.removeRangeByScore(key, 0, windowStart);

        Long currentCount = zSetOps.count(key, windowStart, currentTime);

        if (currentCount != null && currentCount >= MAX_REQUESTS) {
            return false;
        }

        zSetOps.add(key, String.valueOf(currentTime), (double) currentTime);
        stringRedisTemplate.expire(key, WINDOW_SIZE);

        return true;
    }
}
