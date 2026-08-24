package com.sihan.local_review_platform.controller;


import com.sihan.local_review_platform.common.ApiResponse;
import com.sihan.local_review_platform.dto.SeckillResponse;
import com.sihan.local_review_platform.service.VoucherOrderService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/voucher")
public class VoucherOrderController {
    private final VoucherOrderService voucherOrderService;

    public VoucherOrderController(
            VoucherOrderService voucherOrderService
    ){
        this.voucherOrderService = voucherOrderService;
    }

    @PostMapping("/seckill/{voucherId}")
    public ApiResponse<SeckillResponse> seckillVoucher(
            @PathVariable Long voucherId
    ){
        return ApiResponse.ok(voucherOrderService.seckillVoucher(voucherId));
    }
}
