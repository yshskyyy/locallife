package com.sihan.local_review_platform.utils;

public final class RedisKeys {
    public static final String LOGIN_CODE = "auth:code:";
    public static final String LOGIN_TOKEN = "auth:token:";
    public static final String SIGN = "user:sign:";
    public static final String BUSINESS_CACHE = "shop:detail:";
    public static final String BUSINESS_LOCK = "lock:shop:detail:";
    public static final String BUSINESS_GEO = "shop:geo:all";
    public static final String REVIEW_LIKE = "shop:review:likes:";
    public static final String SECKILL_STOCK = "seckill:stock:";
    public static final String SECKILL_ORDER = "seckill:order:";
    public static final String STREAM_RETRY = "stream:orders:retry";
    public static final String STREAM_DEAD = "stream.orders.dead";

    private RedisKeys() {}
}
