package com.sihan.local_review_platform.repository;

import com.sihan.local_review_platform.entity.Business;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import com.sihan.local_review_platform.entity.BusinessStatus;
import org.springframework.data.repository.query.Param;

public interface BusinessRepository extends JpaRepository<Business,Long> {
    Page<Business> findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCaseOrAddressContainingIgnoreCase(
            String name,
            String category,
            String address,
            Pageable pageable
    );

    Page<Business> findByStatus(BusinessStatus status, Pageable pageable);

    Page<Business> findByMerchantId(Long merchantId, Pageable pageable);

    @Query("""
            select b from Business b where b.status = :status
            order by case when b.rating is null then 1 else 0 end, b.rating desc
            """)
    Page<Business> findByStatusOrderByRatingDesc(@Param("status") BusinessStatus status, Pageable pageable);

    @Query("""
            select b from Business b where b.merchantId = :merchantId
            order by case when b.rating is null then 1 else 0 end, b.rating desc
            """)
    Page<Business> findByMerchantIdOrderByRatingDesc(@Param("merchantId") Long merchantId, Pageable pageable);

    @Query("""
            select b from Business b
            where b.status = :status
              and (lower(b.name) like lower(concat('%', :keyword, '%'))
                   or lower(b.category) like lower(concat('%', :keyword, '%'))
                   or lower(b.address) like lower(concat('%', :keyword, '%')))
            """)
    Page<Business> searchByStatus(@Param("status") BusinessStatus status,
                                  @Param("keyword") String keyword,
                                  Pageable pageable);

    @Query("""
            select b from Business b
            where b.status = :status
              and (lower(b.name) like lower(concat('%', :keyword, '%'))
                   or lower(b.category) like lower(concat('%', :keyword, '%'))
                   or lower(b.address) like lower(concat('%', :keyword, '%')))
            order by case when b.rating is null then 1 else 0 end, b.rating desc
            """)
    Page<Business> searchByStatusOrderByRatingDesc(@Param("status") BusinessStatus status,
                                                   @Param("keyword") String keyword,
                                                   Pageable pageable);

    @Query("""
            select b from Business b
            where b.merchantId = :merchantId
              and (lower(b.name) like lower(concat('%', :keyword, '%'))
                   or lower(b.category) like lower(concat('%', :keyword, '%'))
                   or lower(b.address) like lower(concat('%', :keyword, '%')))
            """)
    Page<Business> searchByMerchantId(@Param("merchantId") Long merchantId,
                                      @Param("keyword") String keyword,
                                      Pageable pageable);

    @Query("""
            select b from Business b
            where b.merchantId = :merchantId
              and (lower(b.name) like lower(concat('%', :keyword, '%'))
                   or lower(b.category) like lower(concat('%', :keyword, '%'))
                   or lower(b.address) like lower(concat('%', :keyword, '%')))
            order by case when b.rating is null then 1 else 0 end, b.rating desc
            """)
    Page<Business> searchByMerchantIdOrderByRatingDesc(@Param("merchantId") Long merchantId,
                                                       @Param("keyword") String keyword,
                                                       Pageable pageable);
}
