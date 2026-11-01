package cz.softiq.service.softiq.entity;

import cz.softiq.common.base.model.IBaseEntity;
import cz.softiq.service.softiq.model.enums.BlogStatus;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "blog_post",
    uniqueConstraints = @UniqueConstraint(name = "uk_blog_post_slug", columnNames = "slug"))
@Data
@NoArgsConstructor
public class BlogPostEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(length = 255)
  private String legacyId;

  @Column(nullable = false, length = 255)
  private String slug;

  @Column(nullable = false, length = 500)
  private String title;

  @Lob
  @Column(nullable = false, columnDefinition = "LONGTEXT")
  private String contentHtml;

  @Lob
  @Column(columnDefinition = "LONGTEXT")
  private String keyTakeaways;

  @Column(length = 4000)
  private String summary;

  @Column(length = 1000)
  private String image;

  @Column(length = 255)
  private String author;

  @Column(length = 10)
  private String language;

  @Column(length = 1000)
  private String metaDescription;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private List<String> metaKeywords = new ArrayList<>();

  @Column(nullable = false)
  private Instant publishDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private BlogStatus status;

  @Column(nullable = false, updatable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Column private Instant publishedAt;

  @PrePersist
  public void onCreate() {
    Instant now = Instant.now();
    if (createdAt == null) {
      createdAt = now;
    }
    updatedAt = now;
  }

  @PreUpdate
  public void onUpdate() {
    updatedAt = Instant.now();
  }
}
