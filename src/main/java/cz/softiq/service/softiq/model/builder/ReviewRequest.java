package cz.softiq.service.softiq.model.builder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public record ReviewRequest(String name, Integer stars, String review, LocalDateTime timestamp) {
  private static final DateTimeFormatter TIMESTAMP_FORMATTER =
      DateTimeFormatter.ofPattern("d.M.yyyy H:mm:ss");

  public static ReviewRequest from(Map<String, Object> requestJson) {
    String name = requiredText(requestJson, "name");
    Integer stars = parseStars(requestJson.get("stars"));
    String review = requiredText(requestJson, "review");

    if (stars < 0 || stars > 5) {
      throw new IllegalArgumentException("Stars must be between 0 and 5.");
    }

    return new ReviewRequest(name, stars, review, LocalDateTime.now());
  }

  private static String requiredText(Map<String, Object> requestJson, String key) {
    Object value = requestJson.get(key);
    if (value == null || value.toString().isBlank()) {
      throw new IllegalArgumentException("Missing required field: " + key);
    }
    return value.toString().trim();
  }

  private static Integer parseStars(Object value) {
    if (value == null) {
      throw new IllegalArgumentException("Missing required field: stars");
    }
    if (value instanceof Number number) {
      return number.intValue();
    }
    try {
      return Integer.parseInt(value.toString());
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Stars must be a number.");
    }
  }

  public String formattedTimestamp() {
    return timestamp != null ? timestamp.format(TIMESTAMP_FORMATTER) : "-";
  }
}
