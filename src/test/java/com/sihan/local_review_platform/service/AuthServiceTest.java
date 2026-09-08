package com.sihan.local_review_platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.dto.LoginRequest;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.User;
import com.sihan.local_review_platform.entity.UserRole;
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
    private static final String PHONE = "0412345678";

    @Test
    void newPhoneWithUserModeCreatesUserAndLogsIn() {
        Fixture f = fixture(Optional.empty());
        when(f.users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        assertNotNull(f.service.login(request("USER")));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(f.users).saveAndFlush(captor.capture());
        assertEquals(UserRole.USER, captor.getValue().getRole());
    }

    @Test
    void newPhoneWithMerchantModeCreatesMerchantAndLogsIn() {
        Fixture f = fixture(Optional.empty());
        when(f.users.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));

        assertNotNull(f.service.login(request("MERCHANT")));

        var captor = org.mockito.ArgumentCaptor.forClass(User.class);
        verify(f.users).saveAndFlush(captor.capture());
        assertEquals(UserRole.MERCHANT, captor.getValue().getRole());
    }

    @Test
    void existingUserWithUserModeLogsIn() {
        Fixture f = fixture(Optional.of(user(UserRole.USER)));
        assertNotNull(f.service.login(request("USER")));
        verify(f.users, never()).save(any());
    }

    @Test
    void existingMerchantWithMerchantModeLogsIn() {
        Fixture f = fixture(Optional.of(user(UserRole.MERCHANT)));
        assertNotNull(f.service.login(request("MERCHANT")));
        verify(f.users, never()).save(any());
    }

    @Test
    void existingUserWithMerchantModeIsForbidden() {
        assertRoleMismatch(UserRole.USER, "MERCHANT", "该账号不是商户账号");
    }

    @Test
    void existingMerchantWithUserModeIsForbidden() {
        assertRoleMismatch(UserRole.MERCHANT, "USER", "该账号是商户账号");
    }

    @Test
    void readsBearerSessionAndRefreshesSlidingTtl() throws Exception {
        UserRepository users = mock(UserRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        ObjectMapper mapper = new ObjectMapper();
        when(values.get(RedisKeys.LOGIN_TOKEN + "abc"))
                .thenReturn(mapper.writeValueAsString(new UserSession(9L, PHONE, "user_5678", UserRole.USER)));
        AuthService service = new AuthService(users, redis, mapper, Duration.ofMinutes(30), Duration.ofMinutes(5));

        UserSession session = service.getSession("Bearer abc", true);

        assertEquals(9L, session.userId());
        verify(redis).expire(RedisKeys.LOGIN_TOKEN + "abc", Duration.ofMinutes(30));
    }

    private void assertRoleMismatch(UserRole storedRole, String loginMode, String messagePart) {
        Fixture f = fixture(Optional.of(user(storedRole)));
        BusinessException error = assertThrows(BusinessException.class, () -> f.service.login(request(loginMode)));
        assertEquals("LOGIN_ROLE_MISMATCH", error.getCode());
        assertEquals(403, error.getStatus().value());
        assertTrue(error.getMessage().contains(messagePart));
        verify(f.redis, never()).delete(RedisKeys.LOGIN_CODE + PHONE);
        verify(f.users, never()).save(any());
    }

    private Fixture fixture(Optional<User> existing) {
        UserRepository users = mock(UserRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(RedisKeys.LOGIN_CODE + PHONE)).thenReturn("123456");
        when(users.findByPhone(PHONE)).thenReturn(existing);
        return new Fixture(users, redis, new AuthService(users, redis, new ObjectMapper(),
                Duration.ofMinutes(30), Duration.ofMinutes(5)));
    }

    private User user(UserRole role) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(81L);
        when(user.getPhone()).thenReturn(PHONE);
        when(user.getNickname()).thenReturn("user_5678");
        when(user.getRole()).thenReturn(role);
        return user;
    }

    private LoginRequest request(String mode) {
        LoginRequest request = new LoginRequest();
        request.setPhone(PHONE);
        request.setCode("123456");
        request.setLoginMode(mode);
        return request;
    }

    private record Fixture(UserRepository users, StringRedisTemplate redis, AuthService service) {}
}
