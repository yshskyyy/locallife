package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.dto.LoginRequest;
import com.sihan.local_review_platform.common.ApiResponse;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.service.AuthService;
import com.sihan.local_review_platform.service.UserService;
import com.sihan.local_review_platform.utils.UserContext;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/user")
public class UserController {
    private final AuthService authService;
    private final UserService userService;

    public UserController(AuthService authService, UserService userService){
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/code")
    public ApiResponse<Map<String,String>> sendCode(@RequestParam String phone){
        authService.sendCode(phone);
        return ApiResponse.ok("Verification code sent", Map.of("developmentCode", "123456"));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String,String>> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(Map.of("token", authService.login(request)));
    }

    @GetMapping("/me")
    public ApiResponse<UserSession> me (){
        return ApiResponse.ok(UserContext.get());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestHeader("Authorization") String token) {
        authService.logout(token);
        return ApiResponse.ok(null);
    }

    @PostMapping("/sign")
    public ApiResponse<Void> sign(){
        userService.sign();
        return ApiResponse.ok("Signed successfully", null);
    }

    @GetMapping("/sign/count")
    public ApiResponse<Integer> signCount() {
        return ApiResponse.ok(userService.signCount());
    }
}
