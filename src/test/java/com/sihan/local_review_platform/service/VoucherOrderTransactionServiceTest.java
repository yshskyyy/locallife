package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.entity.VoucherOrder;
import com.sihan.local_review_platform.repository.VoucherOrderRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class VoucherOrderTransactionServiceTest {
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final VoucherOrderRepository orders = mock(VoucherOrderRepository.class);
    private final VoucherOrderTransactionService service = new VoucherOrderTransactionService(vouchers, orders);

    @Test
    void atomicallyDecrementsStockAndCreatesOrder() {
        when(orders.existsByUserIdAndVoucherId(7L, 9L)).thenReturn(false);
        when(vouchers.decrementStockIfAvailable(9L)).thenReturn(1);
        when(orders.saveAndFlush(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VoucherOrder order = service.createOrder(9L, 7L);

        assertEquals(7L, order.getUserId());
        assertEquals(9L, order.getVoucherId());
        verify(vouchers).decrementStockIfAvailable(9L);
        verify(orders).saveAndFlush(any(VoucherOrder.class));
    }

    @Test
    void rejectsWhenConditionalStockUpdateAffectsNoRows() {
        when(vouchers.decrementStockIfAvailable(9L)).thenReturn(0);

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.createOrder(9L, 7L));

        assertEquals("OUT_OF_STOCK", error.getCode());
        verify(orders, never()).saveAndFlush(any());
    }

    @Test
    void duplicateDeliveryReturnsExistingOrderWithoutDecrementingAgain() {
        VoucherOrder existing = new VoucherOrder();
        existing.setUserId(7L);
        existing.setVoucherId(9L);
        when(orders.existsByUserIdAndVoucherId(7L, 9L)).thenReturn(true);
        when(orders.findByUserIdAndVoucherId(7L, 9L)).thenReturn(Optional.of(existing));

        assertSame(existing, service.createOrder(9L, 7L));
        verifyNoInteractions(vouchers);
    }
}
