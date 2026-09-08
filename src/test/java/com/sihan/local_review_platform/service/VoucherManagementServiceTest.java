package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.dto.CreateVoucherRequest;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.VoucherOrderRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoucherManagementServiceTest {
    private final BusinessRepository businesses = mock(BusinessRepository.class);
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final VoucherOrderRepository orders = mock(VoucherOrderRepository.class);
    private final VoucherManagementService service = new VoucherManagementService(businesses, vouchers, orders);

    @Test
    void createsVoucherWhenRangeIsValidAndEndIsFuture() {
        Business business = mock(Business.class);
        when(business.getId()).thenReturn(3L);
        when(business.getName()).thenReturn("测试门店");
        when(business.getStatus()).thenReturn(BusinessStatus.ACTIVE);
        when(businesses.findById(3L)).thenReturn(Optional.of(business));
        when(vouchers.save(any())).thenAnswer(call -> call.getArgument(0));
        LocalDateTime now = LocalDateTime.now();

        var response = service.create(3L, request(now.minusMinutes(1), now.plusHours(2)));

        assertEquals("测试活动", response.title());
        assertEquals("ACTIVE", response.status());
        verify(vouchers).save(any());
    }

    @Test
    void rejectsEndNotAfterBegin() {
        LocalDateTime begin = LocalDateTime.now().plusHours(2);
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(3L, request(begin, begin)));
        assertEquals("结束时间必须晚于开始时间", error.getReason());
        verifyNoInteractions(businesses, vouchers, orders);
    }

    @Test
    void rejectsEndAlreadyExpired() {
        LocalDateTime now = LocalDateTime.now();
        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> service.create(3L, request(now.minusHours(2), now.minusHours(1))));
        assertEquals("结束时间不能早于当前时间", error.getReason());
        verifyNoInteractions(businesses, vouchers, orders);
    }

    @Test
    void deletesVoucherWithoutOrders() {
        when(vouchers.existsById(8L)).thenReturn(true);
        when(orders.countByVoucherId(8L)).thenReturn(0L);

        service.delete(8L);

        verify(vouchers).deleteById(8L);
    }

    @Test
    void keepsVoucherAndOrdersWhenVoucherHasOrders() {
        when(vouchers.existsById(8L)).thenReturn(true);
        when(orders.countByVoucherId(8L)).thenReturn(1L);

        var error = assertThrows(com.sihan.local_review_platform.common.BusinessException.class,
                () -> service.delete(8L));

        assertEquals("该营销活动已有领取记录，无法删除", error.getMessage());
        verify(vouchers, never()).deleteById(anyLong());
        verify(orders, never()).delete(any());
    }

    @Test
    void rejectsNewVoucherForClosedBusiness() {
        Business business = mock(Business.class);
        when(business.getStatus()).thenReturn(BusinessStatus.CLOSED);
        when(businesses.findById(3L)).thenReturn(Optional.of(business));
        LocalDateTime now = LocalDateTime.now();

        var error = assertThrows(com.sihan.local_review_platform.common.BusinessException.class,
                () -> service.create(3L, request(now, now.plusHours(1))));

        assertEquals("BUSINESS_CLOSED", error.getCode());
        verify(vouchers, never()).save(any());
    }

    private CreateVoucherRequest request(LocalDateTime begin, LocalDateTime end) {
        return new CreateVoucherRequest("测试活动", 10, begin, end);
    }
}
