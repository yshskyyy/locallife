package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.entity.Voucher;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class VoucherOrderTimeValidationTest {
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final VoucherOrderService service = new VoucherOrderService(vouchers, redis, "stream.orders");

    @AfterEach
    void clear() { UserContext.clear(); }

    @Test
    void rejectsBeforeBeginWithoutRedisSideEffects() {
        prepare(voucher(LocalDateTime.now().plusHours(1), LocalDateTime.now().plusHours(2)));

        BusinessException error = assertThrows(BusinessException.class, () -> service.seckillVoucher(5L));

        assertEquals("NOT_STARTED", error.getCode());
        assertEquals("活动尚未开始", error.getMessage());
        verifyNoInteractions(redis);
    }

    @Test
    void allowsDuringHalfOpenTimeRange() {
        prepare(voucher(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1)));
        @SuppressWarnings("unchecked") ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(redis.execute(any(), anyList(), any(), any(), any(), any())).thenReturn(0L);

        var response = service.seckillVoucher(5L);

        assertEquals("PROCESSING", response.status());
        verify(redis).execute(any(), anyList(), eq("5"), eq("7"), anyString(), eq("stream.orders"));
    }

    @Test
    void rejectsAtOrAfterEndWithoutRedisSideEffects() {
        prepare(voucher(LocalDateTime.now().minusHours(2), LocalDateTime.now().minusNanos(1)));

        BusinessException error = assertThrows(BusinessException.class, () -> service.seckillVoucher(5L));

        assertEquals("ENDED", error.getCode());
        assertEquals("活动已结束", error.getMessage());
        verifyNoInteractions(redis);
    }

    @Test
    void rejectsVoucherFromClosedBusinessWithoutRedisSideEffects() {
        Voucher voucher = voucher(LocalDateTime.now().minusMinutes(1), LocalDateTime.now().plusHours(1));
        voucher.getBusiness().setStatus(BusinessStatus.CLOSED);
        prepare(voucher);

        BusinessException error = assertThrows(BusinessException.class, () -> service.seckillVoucher(5L));

        assertEquals("BUSINESS_CLOSED", error.getCode());
        verifyNoInteractions(redis);
    }

    private void prepare(Voucher voucher) {
        UserContext.set(new UserSession(7L, "0499000007", "user_0007", UserRole.USER));
        when(vouchers.findById(5L)).thenReturn(Optional.of(voucher));
    }

    private Voucher voucher(LocalDateTime begin, LocalDateTime end) {
        Voucher voucher = new Voucher();
        voucher.setId(5L);
        voucher.setStock(10);
        voucher.setBeginTime(begin);
        voucher.setEndTime(end);
        Business business = new Business();
        business.setStatus(BusinessStatus.ACTIVE);
        voucher.setBusiness(business);
        return voucher;
    }
}
