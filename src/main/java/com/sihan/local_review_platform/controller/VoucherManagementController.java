package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.common.ApiResponse;
import com.sihan.local_review_platform.dto.CreateVoucherRequest;
import com.sihan.local_review_platform.dto.VoucherResponse;
import com.sihan.local_review_platform.service.VoucherManagementService;
import com.sihan.local_review_platform.service.RoleGuard;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores/{businessId}/vouchers")
public class VoucherManagementController {
    private final VoucherManagementService voucherManagementService;
    private final RoleGuard roleGuard;

    public VoucherManagementController(VoucherManagementService voucherManagementService, RoleGuard roleGuard) {
        this.voucherManagementService = voucherManagementService;
        this.roleGuard = roleGuard;
    }

    @GetMapping
    public ApiResponse<List<VoucherResponse>> findByBusiness(@PathVariable Long businessId) {
        return ApiResponse.ok(voucherManagementService.findByBusiness(businessId));
    }

    @PostMapping
    public ApiResponse<VoucherResponse> create(@PathVariable Long businessId,
                                               @Valid @RequestBody CreateVoucherRequest request) {
        roleGuard.requireMerchant();
        return ApiResponse.ok(voucherManagementService.create(businessId, request));
    }
}
