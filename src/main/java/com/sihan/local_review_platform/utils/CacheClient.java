package com.sihan.local_review_platform.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Component
public class CacheClient {
    private static final int MAX_LOCK_ATTEMPTS = 20;
    private static final Duration LOCK_TTL = Duration.ofSeconds(10);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                    "return redis.call('del', KEYS[1]) else return 0 end", Long.class);

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;

    public CacheClient(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    public <R, ID> R queryWithMutex(String keyPrefix, String lockPrefix, ID id, Class<R> type,
                                    Function<ID, Optional<R>> dbFallback, Long time, TimeUnit unit) {
        String key = keyPrefix + id;
        String lockKey = lockPrefix + id;

        for (int attempt = 0; attempt < MAX_LOCK_ATTEMPTS; attempt++) {
            R cached = readCache(key, type);
            if (cached != null) return cached;
            if (Boolean.TRUE.equals(redis.hasKey(key))) return null;

            String lockValue = UUID.randomUUID().toString();
            if (tryLock(lockKey, lockValue)) {
                try {
                    cached = readCache(key, type);
                    if (cached != null) return cached;
                    if (Boolean.TRUE.equals(redis.hasKey(key))) return null;

                    Optional<R> optional = dbFallback.apply(id);
                    if (optional.isEmpty()) {
                        redis.opsForValue().set(key, "", Duration.ofMinutes(2));
                        return null;
                    }
                    R result = optional.get();
                    long baseMillis = unit.toMillis(time);
                    long jitter = Math.max(1L, baseMillis / 10);
                    Duration ttl = Duration.ofMillis(baseMillis + ThreadLocalRandom.current().nextLong(jitter));
                    redis.opsForValue().set(key, writeJson(result), ttl);
                    return result;
                } finally {
                    unlock(lockKey, lockValue);
                }
            }
            sleepBeforeRetry(attempt);
        }
        // Bounded fallback: protect availability without an unbounded wait or recursive stack growth.
        return dbFallback.apply(id).orElse(null);
    }

    private <R> R readCache(String key, Class<R> type) {
        String json = redis.opsForValue().get(key);
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            redis.delete(key);
            throw new IllegalStateException("Cache deserialization failed", e);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Cache serialization failed", e);
        }
    }

    private boolean tryLock(String key, String value) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, LOCK_TTL));
    }

    private void unlock(String key, String value) {
        redis.execute(UNLOCK_SCRIPT, List.of(key), value);
    }

    private static void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(Math.min(20L + attempt * 5L, 100L));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while rebuilding cache", e);
        }
    }
}
