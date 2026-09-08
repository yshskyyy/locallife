package com.sihan.local_review_platform.repository;

import com.sihan.local_review_platform.entity.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoucherRepository
        extends JpaRepository<Voucher, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Voucher v set v.stock = v.stock - 1 where v.id = :voucherId and v.stock > 0")
    int decrementStockIfAvailable(@Param("voucherId") Long voucherId);

    List<Voucher> findByBusinessIdOrderByBeginTimeDesc(Long businessId);
    boolean existsByBusinessId(Long businessId);
}
