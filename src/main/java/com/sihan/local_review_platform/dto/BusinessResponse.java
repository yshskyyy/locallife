package com.sihan.local_review_platform.dto;

import java.time.LocalDateTime;
import com.sihan.local_review_platform.entity.BusinessStatus;

public class BusinessResponse {
    private Long id;
    private String name;
    private String address;
    private String category;
    private Double rating;
    private LocalDateTime createAt;
    private Double longitude;
    private Double latitude;
    private Long brandId;
    private String brandName;
    private BusinessStatus status;

    private Double distance;


    public BusinessResponse (
            Long id,
            String name,
            String category,
            String address,
            Double rating,
            LocalDateTime createAt,
            Double longitude,
            Double latitude,
            Long brandId,
            String brandName,
            BusinessStatus status
            ){
        this.id = id;
        this.name = name;
        this.address = address;
        this.category = category;
        this.rating = rating;
        this.createAt = createAt;
        this.longitude = longitude;
        this.latitude = latitude;
        this.brandId = brandId;
        this.brandName = brandName;
        this.status = status;
    }

    public Long getId(){
        return id;
    }

    public String getName(){
        return name;
    }

    public String getCategory(){
        return category;
    }

    public String getAddress(){
        return address;
    }

    public Double getRating(){
        return rating;
    }

    public LocalDateTime getCreatedAt(){
        return createAt;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getDistance() {
        return distance;
    }

    public Long getBrandId() {
        return brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public BusinessStatus getStatus() { return status; }

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
