package cz.softiq.service.realiq.model.records;

import cz.softiq.service.realiq.model.entities.UserActivityEntity;
import cz.softiq.service.realiq.model.enums.UserActivityType;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public record UserActivityItem(
    Long id, UserActivityType type, LocalDateTime createdAt, Map<String, Object> metadata) {

  public static UserActivityItem from(UserActivityEntity entity) {
    return new UserActivityItem(
        entity.getId(),
        entity.getType(),
        entity.getCreatedAt(),
        new LinkedHashMap<>(entity.getMetadata()));
  }
}
