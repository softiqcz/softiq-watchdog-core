package cz.softiq.service.softiq.entity;

import cz.softiq.common.base.model.IBaseEntity;
import cz.softiq.service.softiq.model.builder.ReviewRequest;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "testimonials")
@Data
@NoArgsConstructor
public class TestimonialsEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private Integer stars;

  @Column(nullable = false, length = 4000)
  private String review;

  @Column(nullable = false)
  private Boolean isPublished = false;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  public TestimonialsEntity(ReviewRequest reviewRequest) {
    this.name = reviewRequest.name();
    this.stars = reviewRequest.stars();
    this.review = reviewRequest.review();
    this.isPublished = false;
    this.createdAt = LocalDateTime.now();
  }
}
