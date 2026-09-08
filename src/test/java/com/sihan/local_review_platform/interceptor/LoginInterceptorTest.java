package com.sihan.local_review_platform.interceptor;

import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.service.AuthService;
import com.sihan.local_review_platform.utils.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LoginInterceptorTest {
    @AfterEach
    void clear() { UserContext.clear(); }

    @Test
    void restoresAndAlwaysClearsRealUserContext() throws Exception {
        AuthService auth = mock(AuthService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("Bearer token");
        when(auth.getSession("Bearer token", true)).thenReturn(new UserSession(42L, "0412345678", "user_5678", UserRole.USER));
        LoginInterceptor interceptor = new LoginInterceptor(auth);

        assertTrue(interceptor.preHandle(request, response, new Object()));
        assertEquals(42L, UserContext.requireUserId());

        interceptor.afterCompletion(request, response, new Object(), null);
        assertNull(UserContext.get());
    }

    @Test
    void rejectsMissingSession() throws Exception {
        AuthService auth = mock(AuthService.class);
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        when(request.getHeader("Authorization")).thenReturn("bad-token");
        LoginInterceptor interceptor = new LoginInterceptor(auth);

        assertFalse(interceptor.preHandle(request, response, new Object()));
        verify(response).setStatus(401);
    }
}
