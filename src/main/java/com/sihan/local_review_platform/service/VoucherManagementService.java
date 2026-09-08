package com.sihan.local_review_platform.service;

import com.sihan.local_review_platform.dto.CreateVoucherRequest;
import com.sihan.local_review_platform.dto.VoucherResponse;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.Voucher;
import com.sihan.local_review_platform.entity.BusinessStatus;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.VoucherOrderRepository;
import com.sihan.local_review_platform.repository.VoucherRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sihan.local_review_platform.common.BusinessException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VoucherManagementService {
    private final BusinessRepository businessRepository;
    private final VoucherRepository voucherRepository;
    private final VoucherOrderRepository orderRepository;

    public VoucherManagementService(BusinessRepository businessRepository,
                                    VoucherRepository voucherRepository,
                                    VoucherOrderRepository orderRepository) {
        this.businessRepository = businessRepository;
        this.voucherRepository = voucherRepository;
        this.orderRepository = orderRepository;
    }

    public VoucherResponse create(Long businessId, CreateVoucherRequest request) {
        if (!request.endTime().isAfter(request.beginTime())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间必须晚于开始时间");
        }
        if (!request.endTime().isAfter(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "结束时间不能早于当前时间");
        }
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));
        if (business.getStatus() != BusinessStatus.ACTIVE) {
            throw new BusinessException("BUSINESS_CLOSED", "门店已关闭，无法创建营销活动", HttpStatus.CONFLICT);
        }
        Voucher voucher = new Voucher();
        voucher.setBusiness(business);
        voucher.setTitle(request.title());
        voucher.setStock(request.stock());
        voucher.setBeginTime(request.beginTime());
        voucher.setEndTime(request.endTime());
        return toResponse(voucherRepository.save(voucher));
    }

    public List<VoucherResponse> findByBusiness(Long businessId) {
        if (!businessRepository.existsById(businessId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found");
        }
        return voucherRepository.findByBusinessIdOrderByBeginTimeDesc(businessId)
                .stream().map(this::toResponse).toList();
    }

    public void delete(Long voucherId) {
        if (!voucherRepository.existsById(voucherId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "营销活动不存在");
        }
        if (orderRepository.countByVoucherId(voucherId) > 0) {
            throw new BusinessException("VOUCHER_HAS_ORDERS",
                    "该营销活动已有领取记录，无法删除", HttpStatus.CONFLICT);
        }
        voucherRepository.deleteById(voucherId);
    }

    private VoucherResponse toResponse(Voucher voucher) {
        LocalDateTime now = LocalDateTime.now();
        String status = now.isBefore(voucher.getBeginTime()) ? "SCHEDULED"
                : !now.isBefore(voucher.getEndTime()) ? "ENDED"
                : voucher.getStock() <= 0 ? "SOLD_OUT" : "ACTIVE";
        return new VoucherResponse(
                voucher.getId(), voucher.getBusiness().getId(), voucher.getBusiness().getName(),
                voucher.getTitle(), voucher.getStock(), voucher.getBeginTime(), voucher.getEndTime(),
                orderRepository.countByVoucherId(voucher.getId()), status);
    }
}
