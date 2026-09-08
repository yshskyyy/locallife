package com.sihan.local_review_platform.repository;

import com.sihan.local_review_platform.entity.VoucherOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface VoucherOrderRepository extends JpaRepository<VoucherOrder, Long> {

    int countByUserIdAndVoucherId(Long userId, Long voucherId);
    boolean existsByUserIdAndVoucherId(Long userId, Long voucherId);
    Optional<VoucherOrder> findByUserIdAndVoucherId(Long userId, Long voucherId);
    long countByVoucherId(Long voucherId);
    List<VoucherOrder> findByUserIdOrderByCreateTimeDesc(Long userId);
}
