package WhatTheBus.Service;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import java.time.Duration;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitService")
class RateLimitServiceTest {

    private static final String CLIENT_ID = "hashedClient123";
    private static final String KEY = "rate_limit:" + CLIENT_ID;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    @Mock
    private CircuitBreaker circuitBreaker;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        lenient().when(stringRedisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(circuitBreaker.executeSupplier(any()))
                .thenAnswer(invocation -> {
                    @SuppressWarnings("unchecked")
                    Supplier<Boolean> supplier = invocation.getArgument(0);
                    return supplier.get();
                });
    }

    @Nested
    @DisplayName("isAllowed")
    class IsAllowed {

        @Test
        @DisplayName("윈도우 내 요청 수가 임계치 미만이면 true")
        void returnsTrueWhenUnderLimit() {
            when(zSetOperations.count(eq(KEY), anyDouble(), anyDouble())).thenReturn(5L);

            boolean allowed = rateLimitService.isAllowed(CLIENT_ID);

            assertTrue(allowed);
            verify(zSetOperations).removeRangeByScore(eq(KEY), anyDouble(), anyDouble());
            verify(zSetOperations).add(eq(KEY), anyString(), anyDouble());
            verify(stringRedisTemplate).expire(eq(KEY), eq(Duration.ofMinutes(1)));
        }

        @Test
        @DisplayName("요청 수가 임계치 이상이면 false")
        void returnsFalseWhenAtThreshold() {
            when(zSetOperations.count(eq(KEY), anyDouble(), anyDouble())).thenReturn(100L);

            boolean allowed = rateLimitService.isAllowed(CLIENT_ID);

            assertFalse(allowed);
            verify(zSetOperations, never()).add(eq(KEY), anyString(), anyDouble());
        }

        @Test
        @DisplayName("요청 수가 임계치를 초과하면 false")
        void returnsFalseWhenOverLimit() {
            when(zSetOperations.count(eq(KEY), anyDouble(), anyDouble())).thenReturn(150L);

            boolean allowed = rateLimitService.isAllowed(CLIENT_ID);

            assertFalse(allowed);
        }

        @Test
        @DisplayName("Redis 예외 발생 시 fallback 으로 true")
        void returnsTrueOnRedisException() {
            when(zSetOperations.count(eq(KEY), anyDouble(), anyDouble()))
                    .thenThrow(new RuntimeException("Redis down"));

            boolean allowed = rateLimitService.isAllowed(CLIENT_ID);

            assertTrue(allowed);
        }

        @Test
        @DisplayName("이전 요청 정리는 removeRangeByScore 로 수행된다")
        void cleansUpOldEntries() {
            when(zSetOperations.count(eq(KEY), anyDouble(), anyDouble())).thenReturn(5L);

            rateLimitService.isAllowed(CLIENT_ID);

            verify(zSetOperations).removeRangeByScore(eq(KEY), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("TTL 을 설정한다")
        void setsExpiration() {
            when(zSetOperations.count(eq(KEY), anyDouble(), anyDouble())).thenReturn(5L);

            rateLimitService.isAllowed(CLIENT_ID);

            verify(stringRedisTemplate).expire(eq(KEY), eq(Duration.ofMinutes(1)));
        }

        @Test
        @DisplayName("clientId 가 null 이면 false")
        void returnsFalseForNullClientId() {
            boolean allowed = rateLimitService.isAllowed(null);

            assertFalse(allowed);
            verifyNoInteractions(circuitBreaker);
            verify(zSetOperations, never()).count(anyString(), anyDouble(), anyDouble());
        }

        @Test
        @DisplayName("clientId 가 빈 문자열이면 false")
        void returnsFalseForEmptyClientId() {
            boolean allowed = rateLimitService.isAllowed(" ");

            assertFalse(allowed);
            verifyNoInteractions(circuitBreaker);
            verify(zSetOperations, never()).count(anyString(), anyDouble(), anyDouble());
        }
    }
}