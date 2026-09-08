package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.dto.MyVoucherResponse;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.entity.Voucher;
import com.sihan.local_review_platform.entity.VoucherOrder;
import com.sihan.local_review_platform.repository.VoucherOrderRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class MyVoucherServiceTest {
    private final VoucherOrderRepository orders = mock(VoucherOrderRepository.class);
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final MyVoucherService service = new MyVoucherService(orders, vouchers);

    @AfterEach
    void clearContext() { UserContext.clear(); }

    @Test
    void returnsOnlyCurrentUsersAvailableVoucher() {
        UserContext.set(new UserSession(7L, "0499000007", "user_0007", UserRole.USER));
        stubVoucher(LocalDateTime.now().plusHours(1));

        List<MyVoucherResponse> result = service.findMine();

        assertEquals(1, result.size());
        assertEquals("AVAILABLE", result.get(0).status());
        verify(orders).findByUserIdOrderByCreateTimeDesc(7L);
        verify(orders, never()).findByUserIdOrderByCreateTimeDesc(8L);
    }

    @Test
    void excludesVoucherExpiredAfterEndTime() {
        UserContext.set(new UserSession(7L, "0499000007", "user_0007", UserRole.USER));
        stubVoucher(LocalDateTime.now().minusMinutes(1));

        assertEquals(0, service.findMine().size());
    }

    @Test
    void excludesVoucherFromClosedBusiness() {
        UserContext.set(new UserSession(7L, "0499000007", "user_0007", UserRole.USER));
        stubVoucher(LocalDateTime.now().plusHours(1), BusinessStatus.CLOSED);

        assertEquals(0, service.findMine().size());
    }

    private void stubVoucher(LocalDateTime endTime) {
        stubVoucher(endTime, BusinessStatus.ACTIVE);
    }

    private void stubVoucher(LocalDateTime endTime, BusinessStatus status) {
        VoucherOrder order = new VoucherOrder();
        order.setId(11L); order.setUserId(7L); order.setVoucherId(5L);
        Voucher voucher = new Voucher();
        voucher.setId(5L); voucher.setTitle("Tea Deal"); voucher.setBeginTime(LocalDateTime.now().minusMinutes(5)); voucher.setEndTime(endTime);
        Business business = new Business(); business.setId(3L); business.setName("Tea Store"); business.setStatus(status); voucher.setBusiness(business);
        when(orders.findByUserIdOrderByCreateTimeDesc(7L)).thenReturn(List.of(order));
        when(vouchers.findById(5L)).thenReturn(Optional.of(voucher));
    }
}
