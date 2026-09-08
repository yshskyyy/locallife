package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.utils.RedisKeys;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.context.event.ContextClosedEvent;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class OrderStreamConsumer {
    private static final Logger log = LoggerFactory.getLogger(OrderStreamConsumer.class);
    private static final String CONSUMER = "phase0-consumer";

    private final StringRedisTemplate redis;
    private final VoucherOrderTransactionService transactionService;
    private final String streamKey;
    private final String group;
    private final int maxRetries;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "order-stream-consumer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicBoolean running = new AtomicBoolean(true);

    public OrderStreamConsumer(StringRedisTemplate redis,
                               VoucherOrderTransactionService transactionService,
                               @Value("${app.stream.order-key:stream.orders}") String streamKey,
                               @Value("${app.stream.order-group:order-group}") String group,
                               @Value("${app.stream.max-retries:3}") int maxRetries) {
        this.redis = redis;
        this.transactionService = transactionService;
        this.streamKey = streamKey;
        this.group = group;
        this.maxRetries = maxRetries;
    }

    @EventListener(ApplicationReadyEvent.class)
    void start() {
        createGroupIfNecessary();
        executor.submit(this::consumeLoop);
    }

    @PreDestroy
    void stop() {
        if (!running.getAndSet(false)) return;
        executor.shutdownNow();
        try {
            executor.awaitTermination(3, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    @EventListener(ContextClosedEvent.class)
    void onContextClosed() {
        stop();
    }

    private void createGroupIfNecessary() {
        try {
            redis.execute((RedisConnection connection) -> connection.execute("XGROUP",
                    bytes("CREATE"), bytes(streamKey), bytes(group), bytes("0-0"), bytes("MKSTREAM")));
        } catch (RuntimeException e) {
            if (!hasCauseMessage(e, "BUSYGROUP")) throw e;
        }
    }

    private void consumeLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                List<MapRecord<String, Object, Object>> pending = read(ReadOffset.from("0"), false);
                if (pending != null && !pending.isEmpty()) {
                    handle(pending.get(0));
                    continue;
                }
                List<MapRecord<String, Object, Object>> fresh = read(ReadOffset.lastConsumed(), true);
                if (fresh != null && !fresh.isEmpty()) handle(fresh.get(0));
            } catch (Exception e) {
                if (running.get()) log.error("Order stream consume loop failed; message remains pending", e);
                pauseAfterInfrastructureFailure();
            }
        }
    }

    private List<MapRecord<String, Object, Object>> read(ReadOffset offset, boolean block) {
        StreamReadOptions options = StreamReadOptions.empty().count(1);
        if (block) options = options.block(Duration.ofSeconds(1));
        return redis.opsForStream().read(Consumer.from(group, CONSUMER), options,
                StreamOffset.create(streamKey, offset));
    }

    private void handle(MapRecord<String, Object, Object> record) {
        Map<Object, Object> values = record.getValue();
        Long userId = null;
        Long voucherId = null;
        try {
            userId = Long.valueOf(required(values, "userId"));
            voucherId = Long.valueOf(required(values, "voucherId"));
            transactionService.createOrder(voucherId, userId);
            acknowledge(record);
            redis.opsForHash().delete(RedisKeys.STREAM_RETRY, record.getId().getValue());
        } catch (Exception failure) {
            long attempts = redis.opsForHash().increment(RedisKeys.STREAM_RETRY, record.getId().getValue(), 1);
            log.warn("Order message {} failed on attempt {}/{}", record.getId(), attempts, maxRetries, failure);
            if (attempts == maxRetries) {
                log.error("Order message {} reached the retry warning threshold and remains pending", record.getId());
            }
            pauseAfterInfrastructureFailure();
        }
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redis.opsForStream().acknowledge(streamKey, group, record.getId());
    }

    private static String required(Map<Object, Object> values, String key) {
        Object value = values.get(key);
        if (value == null) throw new IllegalArgumentException("Missing stream field: " + key);
        return value.toString();
    }

    private void pauseAfterInfrastructureFailure() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    private static boolean hasCauseMessage(Throwable error, String expected) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current.getMessage() != null && current.getMessage().contains(expected)) return true;
        }
        return false;
    }
}
