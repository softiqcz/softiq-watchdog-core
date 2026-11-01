package cz.softiq.service.softiq.model.projection;

import java.time.Instant;

public interface BlogPostPreviewProjection {
  Long getId();

  String getLegacyId();

  String getSlug();

  String getTitle();

  String getSummary();

  String getImage();

  Instant getPublishDate();

  Instant getUpdatedAt();
}
