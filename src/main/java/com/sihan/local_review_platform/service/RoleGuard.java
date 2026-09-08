package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.utils.UserContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoleGuard {
    public void requireMerchant() {
        UserSession user = UserContext.get();
        if (user == null || user.role() != UserRole.MERCHANT) {
            throw new BusinessException("FORBIDDEN", "Merchant role required", HttpStatus.FORBIDDEN);
        }
    }
}
