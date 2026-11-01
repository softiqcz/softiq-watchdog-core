package cz.softiq.service.softiq.model.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.softiq.service.softiq.model.projection.BlogPostPreviewProjection;
import java.time.Instant;

public record BlogPostPreview(
    String id,
    @JsonProperty("url") String slug,
    String title,
    String summary,
    String image,
    @JsonProperty("publish_date") Instant publishDate,
    @JsonProperty("date_modified") Instant dateModified) {

  public static BlogPostPreview from(BlogPostPreviewProjection projection) {
    String responseId =
        projection.getLegacyId() == null
            ? String.valueOf(projection.getId())
            : projection.getLegacyId();
    return new BlogPostPreview(
        responseId,
        projection.getSlug(),
        projection.getTitle(),
        projection.getSummary(),
        projection.getImage(),
        projection.getPublishDate(),
        projection.getUpdatedAt());
  }
}
