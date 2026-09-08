package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.dto.BusinessResponse;
import com.sihan.local_review_platform.dto.CreateBusinessRequest;
import com.sihan.local_review_platform.dto.UserSession;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.repository.BrandRepository;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.utils.CacheClient;
import com.sihan.local_review_platform.utils.UserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class BusinessVisibilityServiceTest {
    private final BusinessRepository businesses = mock(BusinessRepository.class);
    private final BusinessService service = new BusinessService(businesses, mock(StringRedisTemplate.class),
            mock(CacheClient.class), mock(BrandRepository.class), mock(VoucherRepository.class));

    @AfterEach
    void clear() { UserContext.clear(); }

    @Test
    void nullKeywordQueriesOnlyActiveBusinessesWithoutSearchExpression() {
        UserContext.set(new UserSession(7L, "0499000007", "user", UserRole.USER));
        Business active = new Business(); active.setId(1L); active.setName("营业门店"); active.setStatus(BusinessStatus.ACTIVE);
        when(businesses.findByStatus(eq(BusinessStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(active)));

        List<BusinessResponse> result = service.getAllBusinesses(null, 0, 10, "name,asc").getContent();

        assertEquals(1, result.size());
        assertEquals(BusinessStatus.ACTIVE, result.get(0).getStatus());
        verify(businesses).findByStatus(eq(BusinessStatus.ACTIVE), any(Pageable.class));
        verify(businesses, never()).searchByStatus(any(), anyString(), any());
    }

    @Test
    void blankKeywordQueriesOnlyActiveBusinessesWithoutSearchExpression() {
        UserContext.set(new UserSession(7L, "0499000007", "user", UserRole.USER));
        when(businesses.findByStatus(eq(BusinessStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllBusinesses("", 0, 10, "name,asc");

        verify(businesses).findByStatus(eq(BusinessStatus.ACTIVE), any(Pageable.class));
        verify(businesses, never()).searchByStatus(any(), anyString(), any());
    }

    @Test
    void normalKeywordUsesTypedActiveBusinessSearch() {
        UserContext.set(new UserSession(7L, "0499000007", "user", UserRole.USER));
        when(businesses.searchByStatus(eq(BusinessStatus.ACTIVE), eq("茶店"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllBusinesses("  茶店  ", 0, 10, "name,asc");

        verify(businesses).searchByStatus(eq(BusinessStatus.ACTIVE), eq("茶店"), any(Pageable.class));
        verify(businesses, never()).findByStatus(any(), any());
    }

    @Test
    void merchantQueryReturnsOwnBusinessesRegardlessOfStatus() {
        UserContext.set(new UserSession(9L, "0499000009", "merchant", UserRole.MERCHANT));
        when(businesses.findByMerchantId(eq(9L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        service.getAllBusinesses(null, 0, 10, "name,asc");

        verify(businesses).findByMerchantId(eq(9L), any(Pageable.class));
    }

    @Test
    void newBusinessHasNoSyntheticRating() {
        UserContext.set(new UserSession(9L, "0499000009", "merchant", UserRole.MERCHANT));
        CreateBusinessRequest request = mock(CreateBusinessRequest.class);
        when(request.getName()).thenReturn("无评分门店");
        when(request.getCategory()).thenReturn("茶饮");
        when(request.getAddress()).thenReturn("测试地址");
        when(request.getBrandId()).thenReturn(null);
        when(request.getLatitude()).thenReturn(null);
        when(request.getLongitude()).thenReturn(null);
        when(businesses.save(any(Business.class))).thenAnswer(invocation -> {
            Business saved = invocation.getArgument(0);
            saved.setId(20L);
            return saved;
        });

        BusinessResponse response = service.createBusiness(request);

        assertNull(response.getRating());
    }

    @Test
    void nullRatingUsesExplicitSafeRatingQueryAndSerializesNormally() throws Exception {
        UserContext.set(new UserSession(7L, "0499000007", "user", UserRole.USER));
        Business unrated = new Business();
        unrated.setId(21L); unrated.setName("暂无评分门店"); unrated.setStatus(BusinessStatus.ACTIVE);
        when(businesses.findByStatusOrderByRatingDesc(eq(BusinessStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unrated)));

        BusinessResponse response = service.getAllBusinesses(null, 0, 10, "rating,desc").getContent().get(0);

        verify(businesses).findByStatusOrderByRatingDesc(eq(BusinessStatus.ACTIVE), any(Pageable.class));
        assertNull(response.getRating());
        assertTrue(new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(response)
                .contains("\"rating\":null"));
    }
}
