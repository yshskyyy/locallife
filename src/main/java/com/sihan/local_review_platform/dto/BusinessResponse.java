package com.sihan.local_review_platform.dto;

import java.time.LocalDateTime;

public class BusinessResponse {
    private Long id;
    private String name;
    private String address;
    private String category;
    private Double rating;
    private LocalDateTime createAt;
    private Double longitude;
    private Double latitude;

    private Double distance;


    public BusinessResponse (
            Long id,
            String name,
            String category,
            String address,
            Double rating,
            LocalDateTime createAt,
            Double longitude,
            Double latitude
            ){
        this.id = id;
        this.name = name;
        this.address = address;
        this.category = category;
        this.rating = rating;
        this.createAt = createAt;
        this.longitude = longitude;
        this.latitude = latitude;
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

    public void setDistance(Double distance) {
        this.distance = distance;
    }
}
