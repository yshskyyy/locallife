package com.sihan.local_review_platform.interceptor;

import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.service.AuthService;
import com.sihan.local_review_platform.utils.UserContext;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class LoginInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public LoginInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (token == null || token.isBlank()) {
            response.setStatus(401);
            return false;
        }

        UserSession session = authService.getSession(token, true);
        if (session == null) {
            response.setStatus(401);
            return false;
        }

        UserContext.set(session);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        UserContext.clear();
    }
}
