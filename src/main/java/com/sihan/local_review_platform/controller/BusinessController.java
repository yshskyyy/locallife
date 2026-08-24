package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.dto.BusinessResponse;
import com.sihan.local_review_platform.dto.CreateBusinessRequest;
import com.sihan.local_review_platform.dto.UpdateBusinessRequest;
import com.sihan.local_review_platform.service.BusinessService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import com.sihan.local_review_platform.common.ApiResponse;


@RestController
@RequestMapping("/api/businesses")
public class BusinessController {
    private final BusinessService businessService;

    public BusinessController(BusinessService businessService){
        this.businessService = businessService;
    }

    @PostMapping
    public ApiResponse<BusinessResponse> createBusiness(@Valid @RequestBody CreateBusinessRequest request){
        return ApiResponse.ok(businessService.createBusiness(request));
    }

    @GetMapping
    public ApiResponse<Page<BusinessResponse>> getAllBusinesses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "rating,desc") String sort
    ){
        return ApiResponse.ok(businessService.getAllBusinesses(keyword,page,size,sort));
    }

    @GetMapping("/{id}")
    public ApiResponse<BusinessResponse> getBusinessById(@PathVariable Long id){
        return ApiResponse.ok(businessService.getBusinessById(id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteBusiness(@PathVariable Long id){
        businessService.deleteBusiness(id);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<BusinessResponse> updatedBusiness(@PathVariable Long id, @RequestBody UpdateBusinessRequest request){
        return ApiResponse.ok(businessService.updateBusiness(id,request));
    }

    @PostMapping("/geo/load")
    public ApiResponse<Void> loadBusinessGeoToRedis(){
        businessService.loadBusinessGeoToRedis();
        return ApiResponse.ok("Business GEO loaded to Redis", null);
    }

    @GetMapping("/nearby")
    public ApiResponse<List<BusinessResponse>> findNearbyBusinesses(
            @RequestParam Double longitude,
            @RequestParam Double latitude,
            @RequestParam(defaultValue = "5") Double radius
    ) {
        return ApiResponse.ok(businessService.findNearbyBusinesses(longitude, latitude, radius));
    }
}
