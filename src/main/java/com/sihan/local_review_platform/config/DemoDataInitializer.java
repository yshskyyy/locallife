package com.sihan.local_review_platform.config;

import com.sihan.local_review_platform.entity.Brand;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.entity.Voucher;
import com.sihan.local_review_platform.repository.BrandRepository;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import com.sihan.local_review_platform.service.BusinessService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DemoDataInitializer {

    private final BrandRepository brandRepository;
    private final BusinessRepository businessRepository;
    private final VoucherRepository voucherRepository;
    private final BusinessService businessService;

    @Value("${app.demo-data-enabled:true}")
    private boolean demoDataEnabled;

    public DemoDataInitializer(
            BrandRepository brandRepository,
            BusinessRepository businessRepository,
            VoucherRepository voucherRepository,
            BusinessService businessService
    ) {
        this.brandRepository = brandRepository;
        this.businessRepository = businessRepository;
        this.voucherRepository = voucherRepository;
        this.businessService = businessService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeDemoData() {
        if (!demoDataEnabled) {
            return;
        }

        boolean exists = brandRepository.findAll().stream()
                .anyMatch(brand -> "快茶".equals(brand.getName()));

        if (exists) {
            businessService.loadBusinessGeoToRedis();
            return;
        }

        Brand brand = new Brand();
        brand.setName("快茶");
        brand.setDescription("LocalLife 演示用虚构奶茶品牌");
        brand = brandRepository.save(brand);

        Business nearStore = new Business();
        nearStore.setName("快茶（人民广场店）");
        nearStore.setCategory("奶茶饮品");
        nearStore.setAddress("上海市黄浦区人民广场附近");
        nearStore.setRating(4.7);
        nearStore.setLongitude(121.4745);
        nearStore.setLatitude(31.2310);
        nearStore.setBrand(brand);
        nearStore.setStatus(BusinessStatus.ACTIVE);
        nearStore = businessRepository.save(nearStore);

        Business farStore = new Business();
        farStore.setName("快茶（陆家嘴店）");
        farStore.setCategory("奶茶饮品");
        farStore.setAddress("上海市浦东新区陆家嘴附近");
        farStore.setRating(null);
        farStore.setLongitude(121.5050);
        farStore.setLatitude(31.2395);
        farStore.setBrand(brand);
        farStore.setStatus(BusinessStatus.ACTIVE);
        businessRepository.save(farStore);

        Voucher voucher = new Voucher();
        voucher.setTitle("快茶新人券");
        voucher.setStock(50);
        voucher.setBeginTime(LocalDateTime.now().minusDays(1));
        voucher.setEndTime(LocalDateTime.now().plusDays(30));
        voucher.setBusiness(nearStore);
        voucherRepository.save(voucher);

        businessService.loadBusinessGeoToRedis();
    }
}