package com.sihan.local_review_platform.config;

import com.sihan.local_review_platform.interceptor.LoginInterceptor;
import com.sihan.local_review_platform.service.AuthService;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AuthService authService;

    public WebConfig(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(new LoginInterceptor(authService))
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/user/code",
                        "/api/user/login",
                        "/test/redis",
                        "/error"
                );
    }
}
