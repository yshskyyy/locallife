package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.dto.MyVoucherResponse;
import com.sihan.local_review_platform.entity.Voucher;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.repository.VoucherOrderRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.utils.UserContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.AbstractMap;

@Service
public class MyVoucherService {
    private final VoucherOrderRepository orderRepository;
    private final VoucherRepository voucherRepository;

    public MyVoucherService(VoucherOrderRepository orderRepository, VoucherRepository voucherRepository) {
        this.orderRepository = orderRepository;
        this.voucherRepository = voucherRepository;
    }

    @Transactional(readOnly = true)
    public List<MyVoucherResponse> findMine() {
        LocalDateTime now = LocalDateTime.now();
        return orderRepository.findByUserIdOrderByCreateTimeDesc(UserContext.requireUserId()).stream()
                .map(order -> voucherRepository.findById(order.getVoucherId())
                        .map(voucher -> new AbstractMap.SimpleEntry<>(order, voucher)).orElse(null))
                .filter(entry -> entry != null
                        && entry.getValue().getBusiness().getStatus() == BusinessStatus.ACTIVE
                        && !now.isBefore(entry.getValue().getBeginTime())
                        && now.isBefore(entry.getValue().getEndTime()))
                .map(entry -> {
                    Voucher voucher = entry.getValue();
                    return new MyVoucherResponse(voucher.getId(), voucher.getTitle(),
                            voucher.getBusiness().getId(), voucher.getBusiness().getName(),
                            voucher.getBeginTime(), voucher.getEndTime(), entry.getKey().getId(), "AVAILABLE");
                })
                .toList();
    }
}
