package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.common.ApiResponse;
import com.sihan.local_review_platform.service.RoleGuard;
import com.sihan.local_review_platform.service.VoucherManagementService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vouchers")
public class VoucherAdminController {
    private final VoucherManagementService voucherService;
    private final RoleGuard roleGuard;

    public VoucherAdminController(VoucherManagementService voucherService, RoleGuard roleGuard) {
        this.voucherService = voucherService;
        this.roleGuard = roleGuard;
    }

    @DeleteMapping("/{voucherId}")
    public ApiResponse<Void> delete(@PathVariable Long voucherId) {
        roleGuard.requireMerchant();
        voucherService.delete(voucherId);
        return ApiResponse.ok(null);
    }
}
