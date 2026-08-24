package com.sihan.local_review_platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
public class Business {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String category;

    private String address;

    private Double rating;

    private LocalDateTime createdAt;

    private Double longitude;

    private Double latitude;



    @PrePersist
    private void prePersist(){
        this.createdAt = LocalDateTime.now();
        if (this.rating == null){
            this.rating = 0.0;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category){
        this.category = category;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getRating(){
        return rating;
    }

    public void setRating(Double rating){
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt(){
        return createdAt;
    }

    public Long getId(){
        return id;
    }

    public Double getLongitude(){
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getLatitude(){
        return latitude;
    }

    public void setLatitude(Double latitude){
        this.latitude = latitude;
    }
}
