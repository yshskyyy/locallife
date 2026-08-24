package com.sihan.local_review_platform.service;

import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import com.sihan.local_review_platform.utils.RedisKeys;
import com.sihan.local_review_platform.utils.UserContext;

@Service
public class UserService {
    private final StringRedisTemplate stringRedisTemplate;

    public UserService(StringRedisTemplate stringRedisTemplate){
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public void sign(){
        Long userId = UserContext.requireUserId();

        LocalDate now = LocalDate.now();

        String key = RedisKeys.SIGN + userId + ":" +
                now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        int dayOfMonth = now.getDayOfMonth();

        stringRedisTemplate.opsForValue().setBit(
                key,
                dayOfMonth - 1,
                true
        );
    }

    public int signCount(){
        Long userId = UserContext.requireUserId();

        LocalDate now = LocalDate.now();

        String key = RedisKeys.SIGN + userId + ":"+
                now.format(DateTimeFormatter.ofPattern("yyyyMM"));

        int dayOfMonth = now.getDayOfMonth();

        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
        org.springframework.data.redis.connection.BitFieldSubCommands
                .create()
                .get(org.springframework.data.redis.connection.BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth))
                .valueAt(0)
    );
        if (result == null || result.isEmpty()) {
            return 0;
        }

        Long num = result.get(0);

        if (num == null || num == 0) {
            return 0;
        }

        int count = 0;

        while ((num & 1) == 1) {
            count++;
            num = num >>> 1;
        }

        return count;

    }
}
