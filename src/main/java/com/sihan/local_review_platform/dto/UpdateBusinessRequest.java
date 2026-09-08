package com.sihan.local_review_platform.dto;

public class UpdateBusinessRequest {
    private String name;
    private String category;
    private String address;
    private Double rating;
    private Double longitude;
    private Double latitude;
    private Long brandId;

    public String getName(){
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

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Long getBrandId() {
        return brandId;
    }
}
