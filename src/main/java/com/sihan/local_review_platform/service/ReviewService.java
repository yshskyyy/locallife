package com.sihan.local_review_platform.service;
import java.util.List;
import java.util.Map;

import com.sihan.local_review_platform.dto.CreateReviewRequest;
import com.sihan.local_review_platform.dto.ReviewResponse;
import com.sihan.local_review_platform.entity.Business;
import com.sihan.local_review_platform.entity.Review;
import com.sihan.local_review_platform.repository.BusinessRepository;
import com.sihan.local_review_platform.repository.ReviewRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import com.sihan.local_review_platform.utils.RedisKeys;
import com.sihan.local_review_platform.utils.UserContext;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {
    private final BusinessRepository businessRepository;
    private final ReviewRepository reviewRepository;
    private final StringRedisTemplate stringRedisTemplate;


    public ReviewService(ReviewRepository reviewRepository,
                         BusinessRepository businessRepository,
                         StringRedisTemplate stringRedisTemplate){
        this.reviewRepository = reviewRepository;
        this.businessRepository = businessRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Transactional
    public ReviewResponse createReview(CreateReviewRequest request){
        Business business = businessRepository.findById(request.getBusinessId())
                .orElseThrow(()-> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        Review review = new Review();
        review.setBusinessId(request.getBusinessId());
        review.setRating(request.getRating());
        review.setComment(request.getComment());

        Review savedReview = reviewRepository.save(review);

        updateBusinessAverageRating(business.getId());
        return toResponse(savedReview);
    }

    public List<ReviewResponse> getReviewsByBusinessId(Long businessId) {
        return reviewRepository.findByBusinessId(businessId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private void updateBusinessAverageRating(Long businessId) {
        List<Review> reviews = reviewRepository.findByBusinessId(businessId);

        double average = reviews.stream()
                .mapToDouble(Review::getRating)
                .average()
                .orElse(0.0);

        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Business not found"));

        business.setRating(average);
        businessRepository.save(business);
    }

    private ReviewResponse toResponse(Review review){
        return new ReviewResponse(
                review.getId(),
                review.getBusinessId(),
                review.getRating(),
                review.getComment(),
                review.getCreatedAt()
        );
    }


    public Map<String,Object> likeReview(Long reviewId){
        if (!reviewRepository.existsById(reviewId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Review not found");
        }
        String userId = UserContext.requireUserId().toString();
        String key = RedisKeys.REVIEW_LIKE + reviewId;

        Boolean alreadyLiked = stringRedisTemplate.opsForSet()
                .isMember(key,userId);

        if (Boolean.TRUE.equals(alreadyLiked)){
            stringRedisTemplate.opsForSet().remove(key,userId);

            Long likeCount = stringRedisTemplate.opsForSet().size(key);

            return Map.of(

                    "reviewId",reviewId,
                    "liked",false,
                    "likeCount",likeCount
            );
        }
        else {
            stringRedisTemplate.opsForSet().add(key,userId);
            Long likeCount = stringRedisTemplate.opsForSet().size(key);
            return Map.of(

                    "reviewId",reviewId,
                    "liked",true,
                    "likeCount",likeCount
            );
        }
    }

    public Map<String,Object> getLikeStatus(Long reviewId){
        String userId = UserContext.requireUserId().toString();
        String key = RedisKeys.REVIEW_LIKE + reviewId;
        Boolean liked = stringRedisTemplate.opsForSet()
                .isMember(key,userId);

        Long likeCount = stringRedisTemplate.opsForSet()
                .size(key);
        return Map.of(
                "reviewId",reviewId,
                "liked",Boolean.TRUE.equals(liked),
                "likeCount",likeCount
        );

    }
}
