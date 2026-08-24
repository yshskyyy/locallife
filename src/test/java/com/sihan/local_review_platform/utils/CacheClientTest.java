package com.sihan.local_review_platform.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CacheClientTest {
    @SuppressWarnings("unchecked")
    @Test
    void onlyLockOwnerRebuildsAndCachesValue() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("cache:1")).thenReturn(null);
        when(redis.hasKey("cache:1")).thenReturn(false);
        when(values.setIfAbsent(eq("lock:1"), anyString(), any(Duration.class))).thenReturn(true);
        CacheClient client = new CacheClient(redis, new ObjectMapper());

        String result = client.queryWithMutex("cache:", "lock:", 1L, String.class,
                ignored -> Optional.of("shop"), 30L, TimeUnit.MINUTES);

        assertEquals("shop", result);
        verify(values).set(eq("cache:1"), eq("\"shop\""), any(Duration.class));
        verify(redis).execute(any(), eq(java.util.List.of("lock:1")), anyString());
    }

    @Test
    void cachedNullPreventsDatabasePenetration() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("cache:404")).thenReturn("");
        when(redis.hasKey("cache:404")).thenReturn(true);
        CacheClient client = new CacheClient(redis, new ObjectMapper());

        String value = client.queryWithMutex("cache:", "lock:", 404L, String.class,
                ignored -> { throw new AssertionError("database must not be queried"); }, 30L, TimeUnit.MINUTES);

        assertEquals(null, value);
    }
}
