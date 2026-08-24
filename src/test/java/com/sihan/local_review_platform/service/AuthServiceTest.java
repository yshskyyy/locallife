package com.sihan.local_review_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sihan.local_review_platform.dto.LoginRequest;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.User;
import com.sihan.local_review_platform.repository.UserRepository;
import com.sihan.local_review_platform.utils.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {
    @SuppressWarnings("unchecked")
    @Test
    void loginCreatesRealUserSessionAndConsumesCode() {
        UserRepository users = mock(UserRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(RedisKeys.LOGIN_CODE + "0412345678")).thenReturn("123456");
        User user = mock(User.class);
        when(user.getId()).thenReturn(81L);
        when(user.getPhone()).thenReturn("0412345678");
        when(user.getNickname()).thenReturn("user_5678");
        when(users.findByPhone("0412345678")).thenReturn(Optional.of(user));
        AuthService service = new AuthService(users, redis, new ObjectMapper(),
                Duration.ofMinutes(30), Duration.ofMinutes(5));
        LoginRequest request = new LoginRequest();
        request.setPhone("0412345678");
        request.setCode("123456");

        String token = service.login(request);

        assertNotNull(token);
        verify(values).set(eq(RedisKeys.LOGIN_TOKEN + token), contains("\"userId\":81"),
                eq(Duration.ofMinutes(30)));
        verify(redis).delete(RedisKeys.LOGIN_CODE + "0412345678");
    }

    @Test
    void readsBearerSessionAndRefreshesSlidingTtl() throws Exception {
        UserRepository users = mock(UserRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper mapper = new ObjectMapper();
        when(values.get(RedisKeys.LOGIN_TOKEN + "abc"))
                .thenReturn(mapper.writeValueAsString(new UserSession(9L, "0412345678", "user_5678")));
        AuthService service = new AuthService(users, redis, mapper,
                Duration.ofMinutes(30), Duration.ofMinutes(5));

        UserSession session = service.getSession("Bearer abc", true);

        assertEquals(9L, session.userId());
        verify(redis).expire(RedisKeys.LOGIN_TOKEN + "abc", Duration.ofMinutes(30));
    }
}
