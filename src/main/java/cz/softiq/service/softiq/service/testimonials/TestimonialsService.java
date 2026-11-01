package cz.softiq.service.softiq.service.testimonials;

import cz.softiq.service.softiq.entity.TestimonialsEntity;
import cz.softiq.service.softiq.model.builder.ReviewRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
class TestimonialsService {
  private final TestimonialsComponent testimonialsComponent;

  @Transactional
  TestimonialsEntity saveReview(ReviewRequest reviewRequest) {
    return testimonialsComponent.save(new TestimonialsEntity(reviewRequest));
  }

  List<TestimonialsEntity> getPublishedReviews() {
    return testimonialsComponent.getPublishedReviews();
  }
}
