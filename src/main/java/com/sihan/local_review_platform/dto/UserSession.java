package com.sihan.local_review_platform.dto;

import com.sihan.local_review_platform.entity.UserRole;

public record UserSession(Long userId, String phone, String nickname, UserRole role) {}
