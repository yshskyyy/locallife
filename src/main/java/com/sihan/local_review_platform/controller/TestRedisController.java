package com.sihan.local_review_platform.controller;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.sihan.local_review_platform.common.ApiResponse;

@RestController
public class TestRedisController {
    private final StringRedisTemplate stringRedisTemplate;

    public TestRedisController(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @GetMapping("/test/redis")
    public ApiResponse<String> testRedis(){
        stringRedisTemplate.opsForValue().set("test:name", "sihan");

        return ApiResponse.ok(stringRedisTemplate.opsForValue().get("test:name"));
    }
}

