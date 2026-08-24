package com.sihan.local_review_platform.utils;

import com.sihan.local_review_platform.dto.UserSession;

public final class UserContext {
    private static final ThreadLocal<UserSession> CURRENT_USER = new ThreadLocal<>();

    private UserContext() {}

    public static void set(UserSession user) { CURRENT_USER.set(user); }
    public static UserSession get() { return CURRENT_USER.get(); }
    public static Long requireUserId() {
        UserSession user = CURRENT_USER.get();
        if (user == null) throw new IllegalStateException("Authenticated user context is missing");
        return user.userId();
    }
    public static void clear() { CURRENT_USER.remove(); }
}
