package com.sihan.local_review_platform.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateBusinessRequest {
    @NotBlank(message = "Business name is required")
    private String name;

    @NotBlank(message = "Category is required")
    private String category;

    @NotBlank(message = "Address is required")
    private String address;

    private Double rating;

    private Double longitude;

    private Double latitude;

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public String getAddress() {
        return address;
    }

    public Double getRating() {
        return rating;
    }

    public Double getLatitude(){
        return latitude;
    }

    public Double getLongitude(){
        return longitude;
    }
}
