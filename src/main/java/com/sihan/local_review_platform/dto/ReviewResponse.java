package com.sihan.local_review_platform.dto;

import java.time.LocalDateTime;

public class ReviewResponse {
    private Long id;
    private Long businessId;
    private Double rating;
    private String comment;
    private LocalDateTime createdAt;

    public ReviewResponse(Long id, Long businessId, Double rating, String comment,LocalDateTime createdAt){
        this.id = id;
        this.businessId = businessId;
        this.rating = rating;
        this.comment = comment;
        this.createdAt = createdAt;
    }

    public Long getId(){
        return id;
    }

    public Long getBusinessId(){
        return businessId;
    }

    public Double getRating() {
        return rating;
    }

    public String getComment() {
        return comment;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
