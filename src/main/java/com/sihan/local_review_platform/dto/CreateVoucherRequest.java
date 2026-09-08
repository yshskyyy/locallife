package com.sihan.local_review_platform.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateVoucherRequest(
        @NotBlank(message = "Promotion title is required") String title,
        @NotNull @Min(value = 0, message = "Stock must not be negative") Integer stock,
        @NotNull LocalDateTime beginTime,
        @NotNull LocalDateTime endTime
) {
}
