package com.sihan.local_review_platform.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateBrandRequest(
        @NotBlank(message = "Brand name is required") String name,
        String description
) {
}
