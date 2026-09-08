package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.common.BusinessException;
import com.sihan.local_review_platform.repository.BrandRepository;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.utils.CacheClient;
import com.sihan.local_review_platform.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class BusinessDeletionServiceTest {
    private final BusinessRepository businesses = mock(BusinessRepository.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    private final VoucherRepository vouchers = mock(VoucherRepository.class);
    private final BusinessService service = new BusinessService(businesses, redis, mock(CacheClient.class),
            mock(BrandRepository.class), vouchers);

    @AfterEach
    void clearContext() { UserContext.clear(); }

    @Test
    void deletesBusinessWithoutVouchers() {
        @SuppressWarnings("unchecked") ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zset);
        when(businesses.existsById(4L)).thenReturn(true);
        when(vouchers.existsByBusinessId(4L)).thenReturn(false);

        service.deleteBusiness(4L);

        verify(businesses).deleteById(4L);
    }

    @Test
    void keepsBusinessAndVouchersWhenBusinessHasVoucher() {
        when(businesses.existsById(4L)).thenReturn(true);
        when(vouchers.existsByBusinessId(4L)).thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class, () -> service.deleteBusiness(4L));

        assertEquals("该门店仍有关联营销活动，请先删除营销活动", error.getMessage());
        verify(businesses, never()).deleteById(anyLong());
        verify(vouchers, never()).delete(any());
        verifyNoInteractions(redis);
    }

    @Test
    void closesOwnedBusinessWithoutDeletingHistoricalData() {
        @SuppressWarnings("unchecked") ZSetOperations<String, String> zset = mock(ZSetOperations.class);
        when(redis.opsForZSet()).thenReturn(zset);
        UserContext.set(new UserSession(9L, "0499000009", "merchant", UserRole.MERCHANT));
        Business business = new Business();
        business.setId(4L); business.setName("门店"); business.setMerchantId(9L); business.setStatus(BusinessStatus.ACTIVE);
        when(businesses.findById(4L)).thenReturn(java.util.Optional.of(business));
        when(businesses.save(business)).thenReturn(business);

        var response = service.closeBusiness(4L);

        assertEquals(BusinessStatus.CLOSED, response.getStatus());
        verify(zset).remove(com.sihan.local_review_platform.utils.RedisKeys.BUSINESS_GEO, "4");
        verify(businesses, never()).delete(any());
        verify(vouchers, never()).delete(any());
    }
}
