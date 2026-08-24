package com.sihan.local_review_platform.dto;


import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateReviewRequest {
    @NotNull
    private Long businessId;

    @NotNull
    @Min(1)
    @Max(5)
    private Double rating;

    @NotBlank
    private String comment;

    public Long getBusinessId() {
        return businessId;
    }

    public Double getRating() {
        return rating;
    }

    public String getComment(){
        return comment;
    }
}
