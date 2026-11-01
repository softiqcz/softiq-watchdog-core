package cz.softiq.service.softiq.model.builder;

import cz.softiq.service.softiq.model.enums.BlogStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

public record BlogPostUpdateRequest(
    String slug,
    String title,
    String contentHtml,
    String keyTakeaways,
    String summary,
    String image,
    String author,
    String language,
    String metaDescription,
    List<String> metaKeywords,
    Instant publishDate,
    BlogStatus status) {

  public static BlogPostUpdateRequest from(Map<String, Object> data) {
    Map<String, Object> seo = mapValue(data.get("_seo"));
    return new BlogPostUpdateRequest(
        text(data, "slug", "url"),
        text(data, "title"),
        text(data, "contentHtml", "content_html"),
        text(data, "keyTakeaways", "key_takeaways"),
        text(data, "summary"),
        text(data, "image"),
        text(data, "author"),
        text(data, "language"),
        text(seo, "metaDescription", "meta_description"),
        listValue(seo, "metaKeywords", "meta_keywords"),
        publishDate(data),
        status(data.get("status")));
  }

  private static Instant publishDate(Map<String, Object> data) {
    String value = text(data, "publishDate", "publish_date");
    if (value == null) {
      return null;
    }
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "publishDate must be an ISO-8601 timestamp with an offset, for example "
              + "2026-09-08T06:00:00+02:00.");
    }
  }

  private static BlogStatus status(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    try {
      return BlogStatus.valueOf(value.toString().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("status must be DRAFT, SCHEDULED, or PUBLISHED.");
    }
  }

  private static String text(Map<String, Object> data, String... keys) {
    for (String key : keys) {
      Object value = data.get(key);
      if (value != null && !value.toString().isBlank()) {
        return value.toString().trim();
      }
    }
    return null;
  }

  private static List<String> listValue(Map<String, Object> data, String... keys) {
    for (String key : keys) {
      if (!data.containsKey(key)) {
        continue;
      }
      Object value = data.get(key);
      if (!(value instanceof List<?> values)) {
        throw new IllegalArgumentException("metaKeywords must be an array of strings.");
      }
      return values.stream()
          .map(String::valueOf)
          .map(String::trim)
          .filter(s -> !s.isBlank())
          .toList();
    }
    return null;
  }

  private static Map<String, Object> mapValue(Object value) {
    if (value == null) {
      return Map.of();
    }
    if (!(value instanceof Map<?, ?> rawMap)) {
      throw new IllegalArgumentException("_seo must be an object.");
    }
    java.util.HashMap<String, Object> result = new java.util.HashMap<>();
    rawMap.forEach((key, entryValue) -> result.put(String.valueOf(key), entryValue));
    return result;
  }
}
