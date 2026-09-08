package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.common.ApiResponse;
import com.sihan.local_review_platform.dto.BrandResponse;
import com.sihan.local_review_platform.dto.CreateBrandRequest;
import com.sihan.local_review_platform.service.BrandService;
import com.sihan.local_review_platform.service.RoleGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/brands")
public class BrandController {
    private final BrandService brandService;
    private final RoleGuard roleGuard;

    public BrandController(BrandService brandService, RoleGuard roleGuard) {
        this.brandService = brandService;
        this.roleGuard = roleGuard;
    }

    @GetMapping
    public ApiResponse<List<BrandResponse>> findAll() {
        return ApiResponse.ok(brandService.findAll());
    }

    @PostMapping
    public ApiResponse<BrandResponse> create(@Valid @RequestBody CreateBrandRequest request) {
        roleGuard.requireMerchant();
        return ApiResponse.ok(brandService.create(request));
    }
}
