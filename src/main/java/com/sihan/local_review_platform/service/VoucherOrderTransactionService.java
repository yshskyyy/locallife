package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.entity.VoucherOrder;
import com.sihan.local_review_platform.repository.VoucherOrderRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherOrderTransactionService {
    private final VoucherRepository voucherRepository;
    private final VoucherOrderRepository orderRepository;

    public VoucherOrderTransactionService(VoucherRepository voucherRepository,
                                          VoucherOrderRepository orderRepository) {
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public VoucherOrder createOrder(Long voucherId, Long userId) {
        if (orderRepository.existsByUserIdAndVoucherId(userId, voucherId)) {
            return orderRepository.findByUserIdAndVoucherId(userId, voucherId).orElseThrow();
        }
        if (voucherRepository.decrementStockIfAvailable(voucherId) == 0) {
            throw new BusinessException("OUT_OF_STOCK", "Stock not enough", HttpStatus.BAD_REQUEST);
        }
        VoucherOrder order = new VoucherOrder();
        order.setUserId(userId);
        order.setVoucherId(voucherId);
        return orderRepository.saveAndFlush(order);
    }
}
