package cz.softiq.service.softiq.service.testimonials;

import cz.softiq.service.softiq.entity.TestimonialsEntity;
import cz.softiq.service.softiq.repository.TestimonialsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class TestimonialsComponent {
  private final TestimonialsRepository testimonialsRepository;

  public TestimonialsEntity save(TestimonialsEntity testimonial) {
    return testimonialsRepository.save(testimonial);
  }

  public List<TestimonialsEntity> getPublishedReviews() {
    return testimonialsRepository.findAllByIsPublishedTrueOrderByCreatedAtDesc();
  }
}
