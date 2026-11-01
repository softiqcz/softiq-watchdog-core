package cz.softiq.service.softiq.model.builder;

import cz.softiq.service.softiq.model.enums.BlogStatus;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record BlogPostRequest(
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

  public static BlogPostRequest from(Map<String, Object> data) {
    Map<String, Object> seo = nestedMap(data.get("_seo"));
    String publishDate = requiredText(data, "publishDate", "publish_date");

    return new BlogPostRequest(
        requiredText(data, "slug", "url"),
        requiredText(data, "title"),
        requiredText(data, "contentHtml", "content_html"),
        optionalText(data, "keyTakeaways", "key_takeaways"),
        optionalText(data, "summary"),
        optionalText(data, "image"),
        optionalText(data, "author"),
        optionalText(data, "language"),
        optionalText(seo, "metaDescription", "meta_description"),
        stringList(firstValue(seo, "metaKeywords", "meta_keywords")),
        parsePublishDate(publishDate),
        parseStatus(data.get("status"), BlogStatus.SCHEDULED));
  }

  public static Map<String, Object> requestData(Map<String, Object> request) {
    Object data = request.get("data");
    if (!(data instanceof Map<?, ?> rawData)) {
      throw new IllegalArgumentException("Missing required object: data");
    }
    return stringKeyMap(rawData);
  }

  public static List<Map<String, Object>> requestItems(Map<String, Object> request) {
    Map<String, Object> data = requestData(request);
    Object items = data.get("items");
    if (!(items instanceof List<?> rawItems) || rawItems.isEmpty()) {
      throw new IllegalArgumentException("Field data.items must contain at least one blog post.");
    }

    List<Map<String, Object>> result = new ArrayList<>();
    for (Object item : rawItems) {
      if (!(item instanceof Map<?, ?> rawItem)) {
        throw new IllegalArgumentException("Every data.items entry must be an object.");
      }
      result.add(stringKeyMap(rawItem));
    }
    return result;
  }

  private static Instant parsePublishDate(String value) {
    try {
      return OffsetDateTime.parse(value).toInstant();
    } catch (DateTimeParseException e) {
      throw new IllegalArgumentException(
          "publishDate must be an ISO-8601 timestamp with an offset, for example "
              + "2026-09-08T06:00:00+02:00.");
    }
  }

  private static BlogStatus parseStatus(Object value, BlogStatus defaultStatus) {
    if (value == null || value.toString().isBlank()) {
      return defaultStatus;
    }
    try {
      return BlogStatus.valueOf(value.toString().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("status must be DRAFT, SCHEDULED, or PUBLISHED.");
    }
  }

  private static String requiredText(Map<String, Object> data, String... keys) {
    String value = optionalText(data, keys);
    if (value == null) {
      throw new IllegalArgumentException("Missing required field: " + keys[0]);
    }
    return value;
  }

  private static String optionalText(Map<String, Object> data, String... keys) {
    Object value = firstValue(data, keys);
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    return value.toString().trim();
  }

  private static Object firstValue(Map<String, Object> data, String... keys) {
    for (String key : keys) {
      if (data.containsKey(key)) {
        return data.get(key);
      }
    }
    return null;
  }

  private static List<String> stringList(Object value) {
    if (value == null) {
      return new ArrayList<>();
    }
    if (!(value instanceof List<?> values)) {
      throw new IllegalArgumentException("metaKeywords must be an array of strings.");
    }
    return values.stream()
        .map(String::valueOf)
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .toList();
  }

  private static Map<String, Object> nestedMap(Object value) {
    if (value == null) {
      return Map.of();
    }
    if (!(value instanceof Map<?, ?> rawMap)) {
      throw new IllegalArgumentException("_seo must be an object.");
    }
    return stringKeyMap(rawMap);
  }

  private static Map<String, Object> stringKeyMap(Map<?, ?> rawMap) {
    java.util.HashMap<String, Object> result = new java.util.HashMap<>();
    rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
    return result;
  }
}
