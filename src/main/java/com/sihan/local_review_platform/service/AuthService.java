package com.sihan.local_review_platform.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.dto.LoginRequest;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.User;
import com.sihan.local_review_platform.repository.UserRepository;
import com.sihan.local_review_platform.utils.RedisKeys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final Duration tokenTtl;
    private final Duration codeTtl;

    public AuthService(UserRepository userRepository, StringRedisTemplate redis, ObjectMapper objectMapper,
                       @Value("${app.auth.token-ttl:30m}") Duration tokenTtl,
                       @Value("${app.auth.code-ttl:5m}") Duration codeTtl) {
        this.userRepository = userRepository;
        this.redis = redis;
        this.objectMapper = objectMapper;
        this.tokenTtl = tokenTtl;
        this.codeTtl = codeTtl;
    }

    public void sendCode(String phone) {
        validatePhone(phone);
        // Phase 0 keeps a deterministic development code; production should integrate an SMS provider.
        redis.opsForValue().set(RedisKeys.LOGIN_CODE + phone, "123456", codeTtl);
    }

    @Transactional
    public String login(LoginRequest request) {
        validatePhone(request.getPhone());
        String code = redis.opsForValue().get(RedisKeys.LOGIN_CODE + request.getPhone());
        if (code == null || !code.equals(request.getCode())) {
            throw new BusinessException("INVALID_CODE", "Invalid or expired verification code", HttpStatus.BAD_REQUEST);
        }

        User user = findOrCreateUser(request.getPhone());
        String token = UUID.randomUUID().toString();
        saveSession(token, new UserSession(user.getId(), user.getPhone(), user.getNickname()));
        redis.delete(RedisKeys.LOGIN_CODE + request.getPhone());
        return token;
    }

    public UserSession getSession(String rawToken, boolean refreshTtl) {
        String token = normalizeToken(rawToken);
        if (token == null) return null;
        String json = redis.opsForValue().get(RedisKeys.LOGIN_TOKEN + token);
        if (json == null) return null;
        try {
            UserSession session = objectMapper.readValue(json, UserSession.class);
            if (refreshTtl) redis.expire(RedisKeys.LOGIN_TOKEN + token, tokenTtl);
            return session;
        } catch (JsonProcessingException e) {
            redis.delete(RedisKeys.LOGIN_TOKEN + token);
            throw new IllegalStateException("Invalid login session data", e);
        }
    }

    public void logout(String rawToken) {
        String token = normalizeToken(rawToken);
        if (token != null) redis.delete(RedisKeys.LOGIN_TOKEN + token);
    }

    private User findOrCreateUser(String phone) {
        return userRepository.findByPhone(phone).orElseGet(() -> {
            User user = new User();
            user.setPhone(phone);
            user.setNickname("user_" + phone.substring(Math.max(0, phone.length() - 4)));
            try {
                return userRepository.saveAndFlush(user);
            } catch (DataIntegrityViolationException concurrentCreate) {
                return userRepository.findByPhone(phone).orElseThrow(() -> concurrentCreate);
            }
        });
    }

    private void saveSession(String token, UserSession session) {
        try {
            redis.opsForValue().set(RedisKeys.LOGIN_TOKEN + token,
                    objectMapper.writeValueAsString(session), tokenTtl);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not serialize login session", e);
        }
    }

    private static String normalizeToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) return null;
        String value = rawToken.trim();
        return value.regionMatches(true, 0, "Bearer ", 0, 7) ? value.substring(7).trim() : value;
    }

    private static void validatePhone(String phone) {
        if (phone == null || !phone.matches("^[0-9+][0-9]{5,19}$")) {
            throw new BusinessException("INVALID_PHONE", "Invalid phone number", HttpStatus.BAD_REQUEST);
        }
    }
}
