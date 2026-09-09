package com.sihan.local_review_platform.config;

import com.sihan.local_review_platform.service.BusinessService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BusinessGeoInitializer {

    private final BusinessService businessService;

    public BusinessGeoInitializer(BusinessService businessService) {
        this.businessService = businessService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initializeBusinessGeo() {
        businessService.loadBusinessGeoToRedis();
    }
}