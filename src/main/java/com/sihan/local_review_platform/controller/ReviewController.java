package com.sihan.local_review_platform.controller;

import com.sihan.local_review_platform.dto.CreateReviewRequest;
import com.sihan.local_review_platform.dto.ReviewResponse;
import com.sihan.local_review_platform.entity.Review;
import com.sihan.local_review_platform.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import com.sihan.local_review_platform.common.ApiResponse;


@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    public final ReviewService reviewService;

    public ReviewController(ReviewService reviewService){
        this.reviewService = reviewService;
    }

    @PostMapping
    public ApiResponse<ReviewResponse> createReview(@Valid @RequestBody CreateReviewRequest request){
        return ApiResponse.ok(reviewService.createReview(request));
    }

    @GetMapping
    public ApiResponse<List<ReviewResponse>> getReviewByBusinessId(@RequestParam Long businessId){
        return ApiResponse.ok(reviewService.getReviewsByBusinessId(businessId));
    }

    @PostMapping("/{reviewId}/like")
    public ApiResponse<Map<String,Object>> likeReview(@PathVariable Long reviewId){
        return ApiResponse.ok(reviewService.likeReview(reviewId));
    }

    @GetMapping("/{reviewId}/like-status")
    public ApiResponse<Map<String,Object>> getLikeStatus(@PathVariable Long reviewId){
        return ApiResponse.ok(reviewService.getLikeStatus(reviewId));

    }
}
