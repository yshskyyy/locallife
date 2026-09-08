package com.sihan.local_review_platform.dto;

import java.time.LocalDateTime;

public record MyVoucherResponse(Long voucherId, String voucherTitle, Long storeId, String storeName,
                                LocalDateTime startTime, LocalDateTime endTime, Long orderId, String status) {}
