package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.common.ApiResponse;
import com.sihan.local_review_platform.dto.MyVoucherResponse;
import com.sihan.local_review_platform.service.MyVoucherService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/vouchers")
public class MyVoucherController {
    private final MyVoucherService service;

    public MyVoucherController(MyVoucherService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<List<MyVoucherResponse>> findMine() {
        return ApiResponse.ok(service.findMine());
    }
}
