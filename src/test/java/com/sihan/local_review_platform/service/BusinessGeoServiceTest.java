package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.repository.BrandRepository;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.utils.CacheClient;
import com.sihan.local_review_platform.utils.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessGeoServiceTest {
    @Test
    void nearbyKeepsRedisDistanceOrderAndFiltersClosedDirtyMembers() {
        BusinessRepository businesses = mock(BusinessRepository.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked") GeoOperations<String, String> geo = mock(GeoOperations.class);
        when(redis.opsForGeo()).thenReturn(geo);

        var near = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("1", new Point(121.4737, 31.2304)),
                new Distance(0.8));
        var closed = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("2", new Point(121.48, 31.24)),
                new Distance(1.2));
        var far = new GeoResult<>(
                new RedisGeoCommands.GeoLocation<>("3", new Point(121.50, 31.26)),
                new Distance(3.2));
        when(geo.radius(eq(RedisKeys.BUSINESS_GEO), any(Circle.class),
                any(RedisGeoCommands.GeoRadiusCommandArgs.class)))
                .thenReturn(new GeoResults<>(List.of(near, closed, far)));

        Business first = business(1L, "近店", BusinessStatus.ACTIVE);
        Business dirtyClosed = business(2L, "已关闭", BusinessStatus.CLOSED);
        Business third = business(3L, "远店", BusinessStatus.ACTIVE);
        when(businesses.findById(1L)).thenReturn(Optional.of(first));
        when(businesses.findById(2L)).thenReturn(Optional.of(dirtyClosed));
        when(businesses.findById(3L)).thenReturn(Optional.of(third));
        BusinessService service = new BusinessService(businesses, redis, mock(CacheClient.class),
                mock(BrandRepository.class), mock(VoucherRepository.class));

        var result = service.findNearbyBusinesses(121.4737, 31.2304, 50.0);

        assertEquals(List.of("近店", "远店"), result.stream().map(r -> r.getName()).toList());
        assertEquals(List.of(0.8, 3.2), result.stream().map(r -> r.getDistance()).toList());
    }

    private static Business business(Long id, String name, BusinessStatus status) {
        Business business = new Business();
        business.setId(id);
        business.setName(name);
        business.setStatus(status);
        return business;
    }
}
