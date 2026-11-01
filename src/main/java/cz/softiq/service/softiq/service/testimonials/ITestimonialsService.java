package cz.softiq.service.softiq.service.testimonials;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.softiq.entity.TestimonialsEntity;
import cz.softiq.service.softiq.model.builder.ReviewRequest;
import java.util.List;

public interface ITestimonialsService extends IBeanProvider, IBaseService {

  default TestimonialsEntity saveReview(ReviewRequest reviewRequest) {
    return getTestimonialsService().saveReview(reviewRequest);
  }

  default List<TestimonialsEntity> getPublishedReviews() {
    return getTestimonialsService().getPublishedReviews();
  }

  private TestimonialsService getTestimonialsService() {
    return getBean(TestimonialsService.class);
  }
}
