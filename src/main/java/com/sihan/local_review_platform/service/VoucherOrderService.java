package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.dto.SeckillResponse;
import com.sihan.local_review_platform.entity.Voucher;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.utils.RedisKeys;
import com.sihan.local_review_platform.utils.UserContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VoucherOrderService {
    private static final DefaultRedisScript<Long> SECKILL_SCRIPT;

    static {
        SECKILL_SCRIPT = new DefaultRedisScript<>();
        SECKILL_SCRIPT.setLocation(new ClassPathResource("seckill.lua"));
        SECKILL_SCRIPT.setResultType(Long.class);
    }

    private final VoucherRepository voucherRepository;
    private final StringRedisTemplate redis;
    private final String streamKey;

    public VoucherOrderService(VoucherRepository voucherRepository, StringRedisTemplate redis,
                               @Value("${app.stream.order-key:stream.orders}") String streamKey) {
        this.voucherRepository = voucherRepository;
        this.redis = redis;
        this.streamKey = streamKey;
    }

    public SeckillResponse seckillVoucher(Long voucherId) {
        Long userId = UserContext.requireUserId();
        Voucher voucher = voucherRepository.findById(voucherId)
                .orElseThrow(() -> new BusinessException("VOUCHER_NOT_FOUND", "Voucher not found", HttpStatus.NOT_FOUND));
        if (voucher.getBusiness().getStatus() != BusinessStatus.ACTIVE) {
            throw new BusinessException("BUSINESS_CLOSED", "门店已关闭，无法领取优惠券", HttpStatus.CONFLICT);
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(voucher.getBeginTime())) {
            throw new BusinessException("NOT_STARTED", "活动尚未开始", HttpStatus.BAD_REQUEST);
        }
        if (!now.isBefore(voucher.getEndTime())) {
            throw new BusinessException("ENDED", "活动已结束", HttpStatus.BAD_REQUEST);
        }

        redis.opsForValue().setIfAbsent(RedisKeys.SECKILL_STOCK + voucherId,
                String.valueOf(voucher.getStock()));
        String requestId = UUID.randomUUID().toString();
        Long result = redis.execute(SECKILL_SCRIPT, List.of(), voucherId.toString(),
                userId.toString(), requestId, streamKey);
        if (result == null) {
            throw new BusinessException("SECKILL_UNAVAILABLE", "Seckill service unavailable", HttpStatus.SERVICE_UNAVAILABLE);
        }
        if (result == 1L) {
            throw new BusinessException("OUT_OF_STOCK", "Stock not enough", HttpStatus.BAD_REQUEST);
        }
        if (result == 2L) {
            throw new BusinessException("DUPLICATE_ORDER", "User already bought this voucher", HttpStatus.CONFLICT);
        }
        return new SeckillResponse(requestId, "PROCESSING");
    }
}
