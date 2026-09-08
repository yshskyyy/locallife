package com.sihan.local_review_platform.dto;

import java.time.LocalDateTime;

public record VoucherResponse(
        Long id,
        Long businessId,
        String businessName,
        String title,
        Integer stock,
        LocalDateTime beginTime,
        LocalDateTime endTime,
        long orderCount,
        String status
) {
}
