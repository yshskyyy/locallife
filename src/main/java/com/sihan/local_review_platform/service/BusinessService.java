package com.sihan.local_review_platform.service;
import com.sihan.local_review_platform.utils.CacheClient;

import org.springframework.data.domain.Page;
import com.sihan.local_review_platform.dto.BusinessResponse;
import com.sihan.local_review_platform.dto.CreateBusinessRequest;
import com.sihan.local_review_platform.dto.UpdateBusinessRequest;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.repository.BusinessRepository;

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


@Service
public class BusinessService {
    private final BusinessRepository businessRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final CacheClient cacheClient;

    public BusinessService(
            BusinessRepository businessRepository,
            StringRedisTemplate stringRedisTemplate,
            CacheClient cacheClient) {
        this.businessRepository = businessRepository;
        this.stringRedisTemplate = stringRedisTemplate;
        this.cacheClient = cacheClient;
    }

    public BusinessResponse createBusiness(CreateBusinessRequest request) {
        Business business = new Business();

        business.setName(request.getName());
        business.setCategory(request.getCategory());
        business.setAddress(request.getAddress());
        business.setRating(request.getRating());
        business.setLongitude(request.getLongitude());
        business.setLatitude(request.getLatitude());

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

        Sort sortObj = direction.equalsIgnoreCase("desc")
                ? Sort.by(field).descending()
                : Sort.by(field).ascending();

        Pageable pageable = PageRequest.of(
                page,
                size,
                sortObj
        );

        if (keyword != null && !keyword.isBlank()) {
            return businessRepository
                    .findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrAddressContainingIgnoreCase(
                            keyword,
                            keyword,
                            keyword,
                            pageable
                    )
                    .map(this::toResponse);
        }

        return businessRepository.findAll(pageable)
                .map(this::toResponse);
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
        return toResponse(business);
    }


    public void deleteBusiness(Long id) {
        if (!businessRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business is not found");
        }
        businessRepository.deleteById(id);
        stringRedisTemplate.delete(RedisKeys.BUSINESS_CACHE + id);
        stringRedisTemplate.opsForZSet().remove(
                RedisKeys.BUSINESS_GEO,
                id.toString()
        );
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
        Business saved = businessRepository.save(existingBusiness);
        stringRedisTemplate.delete(RedisKeys.BUSINESS_CACHE + id);

        if (saved.getLongitude() != null &&
        saved.getLatitude() != null){
            stringRedisTemplate.opsForGeo().add(
                    RedisKeys.BUSINESS_GEO,
                    new Point(
                            saved.getLongitude(),
                            saved.getLatitude()
                    ),
                    saved.getId().toString()
            );
        }
        return toResponse(saved);
    }

    public void loadBusinessGeoToRedis() {
        List<Business> businesses = businessRepository.findAll();

        for (Business business : businesses) {
            if (business.getLongitude() != null && business.getLatitude() != null) {
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

                    if (business == null) {
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
                business.getLatitude()
        );
    }
}
