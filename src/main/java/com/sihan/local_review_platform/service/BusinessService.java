package com.sihan.local_review_platform.service;
import com.sihan.local_review_platform.utils.CacheClient;

import org.springframework.data.domain.Page;
import com.sihan.local_review_platform.dto.BusinessResponse;
import com.sihan.local_review_platform.dto.CreateBusinessRequest;
import com.sihan.local_review_platform.dto.UpdateBusinessRequest;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.entity.UserRole;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.BrandRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.common.BusinessException;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.geo.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.connection.RedisGeoCommands;
import com.sihan.local_review_platform.utils.RedisKeys;
import com.sihan.local_review_platform.utils.UserContext;


@Service
public class BusinessService {
    private final BusinessRepository businessRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;
    private final BrandRepository brandRepository;
    private final VoucherRepository voucherRepository;

    public BusinessService(
            BusinessRepository businessRepository,
            StringRedisTemplate stringRedisTemplate,
            CacheClient cacheClient,
            BrandRepository brandRepository,
            VoucherRepository voucherRepository) {
        this.businessRepository = businessRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheClient = cacheClient;
        this.brandRepository = brandRepository;
        this.voucherRepository = voucherRepository;
    }

    public BusinessResponse createBusiness(CreateBusinessRequest request) {
        Business business = new Business();

        business.setName(request.getName());
        business.setCategory(request.getCategory());
        business.setAddress(request.getAddress());
        business.setLongitude(request.getLongitude());
        business.setLatitude(request.getLatitude());
        business.setMerchantId(UserContext.requireUserId());
        business.setStatus(BusinessStatus.ACTIVE);
        if (request.getBrandId() != null) {
            business.setBrand(brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found")));
        }

        Business savedBusiness = businessRepository.save(business);

        if (savedBusiness.getLatitude() != null &&
        savedBusiness.getLongitude() != null){

            stringRedisTemplate.opsForGeo().add(
                    RedisKeys.BUSINESS_GEO,
                    new Point(
                            savedBusiness.getLongitude(),
                            savedBusiness.getLatitude()
                    ),
                    savedBusiness.getId().toString()
            );
        }

        return toResponse(savedBusiness);
    }

    public Page<BusinessResponse> getAllBusinesses(String keyword, int page, int size, String sort) {
        String[] sortParts = sort.split(",");
        String field = sortParts[0];
        String direction;

        if (sortParts.length > 1) {
            direction = sortParts[1];
        } else {
            if (field.equalsIgnoreCase("rating") || field.equalsIgnoreCase("createdAt")) {
                direction = "desc";
            } else {
                direction = "asc";
            }
        }

        boolean ratingDescending = field.equals("rating") && direction.equalsIgnoreCase("desc");
        Sort.Order order = direction.equalsIgnoreCase("desc")
                ? Sort.Order.desc(field)
                : Sort.Order.asc(field);
        Sort sortObj = ratingDescending ? Sort.unsorted() : Sort.by(order);

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObj
        );

        boolean merchant = UserContext.get() != null && UserContext.get().role() == UserRole.MERCHANT;
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        Page<Business> businesses;
        if (merchant) {
            Long merchantId = UserContext.requireUserId();
            if (ratingDescending) {
                businesses = hasKeyword
                        ? businessRepository.searchByMerchantIdOrderByRatingDesc(merchantId, keyword.trim(), pageable)
                        : businessRepository.findByMerchantIdOrderByRatingDesc(merchantId, pageable);
            } else {
                businesses = hasKeyword
                        ? businessRepository.searchByMerchantId(merchantId, keyword.trim(), pageable)
                        : businessRepository.findByMerchantId(merchantId, pageable);
            }
        } else {
            if (ratingDescending) {
                businesses = hasKeyword
                        ? businessRepository.searchByStatusOrderByRatingDesc(BusinessStatus.ACTIVE, keyword.trim(), pageable)
                        : businessRepository.findByStatusOrderByRatingDesc(BusinessStatus.ACTIVE, pageable);
            } else {
                businesses = hasKeyword
                        ? businessRepository.searchByStatus(BusinessStatus.ACTIVE, keyword.trim(), pageable)
                        : businessRepository.findByStatus(BusinessStatus.ACTIVE, pageable);
            }
        }
        return businesses.map(this::toResponse);
    }

    public BusinessResponse getBusinessById(Long id) {
        Business business = cacheClient.queryWithMutex(
                RedisKeys.BUSINESS_CACHE,
                RedisKeys.BUSINESS_LOCK,
                id,
                Business.class,
                businessRepository::findById,
                30L,
                TimeUnit.MINUTES
        );
        if (business == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Business not found"
            );
        }
        boolean owner = UserContext.get() != null && UserContext.get().role() == UserRole.MERCHANT
                && UserContext.requireUserId().equals(business.getMerchantId());
        if (business.getStatus() != BusinessStatus.ACTIVE && !owner) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        return toResponse(business);
    }


    public void deleteBusiness(Long id) {
        if (!businessRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business is not found");
        }
        if (voucherRepository.existsByBusinessId(id)) {
            throw new BusinessException("BUSINESS_HAS_VOUCHERS",
                    "该门店仍有关联营销活动，请先删除营销活动", HttpStatus.CONFLICT);
        }
        businessRepository.deleteById(id);
        stringRedisTemplate.delete(RedisKeys.BUSINESS_CACHE + id);
        stringRedisTemplate.opsForZSet().remove(
                RedisKeys.BUSINESS_GEO,
                id.toString()
        );
    }

    public BusinessResponse closeBusiness(Long id) {
        Business business = businessRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        if (!UserContext.requireUserId().equals(business.getMerchantId())) {
            throw new BusinessException("BUSINESS_NOT_OWNED", "只能关闭自己创建的门店", HttpStatus.FORBIDDEN);
        }
        business.setStatus(BusinessStatus.CLOSED);
        Business saved = businessRepository.save(business);
        stringRedisTemplate.delete(RedisKeys.BUSINESS_CACHE + id);
        stringRedisTemplate.opsForZSet().remove(RedisKeys.BUSINESS_GEO, id.toString());
        return toResponse(saved);
    }

    public BusinessResponse updateBusiness(Long id, UpdateBusinessRequest request) {
        Business existingBusiness = businessRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        if (request.getName() != null) {
            existingBusiness.setName(request.getName());
        }

        if (request.getCategory() != null) {
            existingBusiness.setCategory(request.getCategory());
        }

        if (request.getAddress() != null) {
            existingBusiness.setAddress(request.getAddress());
        }

        if (request.getRating() != null) {
            existingBusiness.setRating(request.getRating());
        }

        if (request.getLongitude() != null) {
            existingBusiness.setLongitude(request.getLongitude());
        }

        if (request.getLatitude() != null) {
            existingBusiness.setLatitude(request.getLatitude());
        }
        if (request.getBrandId() != null) {
            existingBusiness.setBrand(brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Brand not found")));
        }
        Business saved = businessRepository.save(existingBusiness);
        stringRedisTemplate.delete(RedisKeys.BUSINESS_CACHE + id);

        if (saved.getStatus() == BusinessStatus.ACTIVE
                && saved.getLongitude() != null && saved.getLatitude() != null){
            stringRedisTemplate.opsForGeo().add(
                    RedisKeys.BUSINESS_GEO,
                    new Point(
                            saved.getLongitude(),
                            saved.getLatitude()
                    ),
                    saved.getId().toString()
            );
        } else {
            stringRedisTemplate.opsForZSet().remove(RedisKeys.BUSINESS_GEO, saved.getId().toString());
        }
        return toResponse(saved);
    }

    public void loadBusinessGeoToRedis() {
        List<Business> businesses = businessRepository.findAll();

        for (Business business : businesses) {
            if (business.getStatus() == BusinessStatus.ACTIVE
                    && business.getLongitude() != null && business.getLatitude() != null) {
                stringRedisTemplate.opsForGeo().add(
                        RedisKeys.BUSINESS_GEO,
                        new Point(business.getLongitude(), business.getLatitude()),
                        business.getId().toString()
                );
            }
        }

    }

    public List<BusinessResponse> findNearbyBusinesses(
            Double longitude,
            Double latitude,
            Double radius
    ) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                stringRedisTemplate.opsForGeo().radius(
                        RedisKeys.BUSINESS_GEO,
                        new Circle(
                                new Point(longitude, latitude),
                                new Distance(radius, Metrics.KILOMETERS)
                        ),
                        RedisGeoCommands.GeoRadiusCommandArgs
                                .newGeoRadiusArgs()
                                .includeDistance()
                                .sortAscending()
                );
        if (results == null || results.getContent().isEmpty()) {
            return List.of();
        }

        return results.getContent().stream()
                .map(result -> {
                    Long id = Long.valueOf(result.getContent().getName());

                    Business business = businessRepository.findById(id)
                            .orElse(null);

                    if (business == null || business.getStatus() != BusinessStatus.ACTIVE) {
                        return null;
                    }

                    BusinessResponse response = toResponse(business);

                    response.setDistance(result.getDistance().getValue());

                    return response;
                })
                .filter(response -> response != null)
                .toList();
    }


    private BusinessResponse toResponse(Business business) {
        return new BusinessResponse(
                business.getId(),
                business.getName(),
                business.getCategory(),
                business.getAddress(),
                business.getRating(),
                business.getCreatedAt(),
                business.getLongitude(),
                business.getLatitude(),
                business.getBrand() == null ? null : business.getBrand().getId(),
                business.getBrand() == null ? null : business.getBrand().getName(),
                business.getStatus()
        );
    }
}
