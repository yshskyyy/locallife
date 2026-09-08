package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.dto.BrandResponse;
import com.sihan.local_review_platform.dto.CreateBrandRequest;
import com.sihan.local_review_platform.entity.Brand;
import com.sihan.local_review_platform.repository.BrandRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BrandService {
    private final BrandRepository brandRepository;

    public BrandService(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    public BrandResponse create(CreateBrandRequest request) {
        Brand brand = new Brand();
        brand.setName(request.name());
        brand.setDescription(request.description());
        return toResponse(brandRepository.save(brand));
    }

    public List<BrandResponse> findAll() {
        return brandRepository.findAll().stream().map(this::toResponse).toList();
    }

    private BrandResponse toResponse(Brand brand) {
        return new BrandResponse(brand.getId(), brand.getName(), brand.getDescription());
    }
}
