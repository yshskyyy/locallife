package com.sihan.local_review_platform.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "voucher_order", uniqueConstraints =
        @UniqueConstraint(name = "uk_voucher_order_user_voucher", columnNames = {"user_id", "voucher_id"}))
public class VoucherOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long voucherId;

    @Column(nullable = false)
    private LocalDateTime createTime;

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setVoucherId(Long voucherId) {
        this.voucherId = voucherId;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    @PrePersist
    public void prePersist() {
        this.createTime = LocalDateTime.now();
    }
}
