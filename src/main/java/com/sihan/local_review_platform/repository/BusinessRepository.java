package com.sihan.local_review_platform.repository;

import com.sihan.local_review_platform.entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusinessRepository extends JpaRepository<Business,Long> {
    Page<Business> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrAddressContainingIgnoreCase(
            String name,
            String category,
            String address,
            Pageable pageable
    );
}
