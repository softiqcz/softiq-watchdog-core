package cz.softiq.service.realiq.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Arrays;
import java.util.List;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

  @Override
  public String convertToDatabaseColumn(List<String> values) {
    if (values == null || values.isEmpty()) {
      return null;
    }

    return values.stream()
        .filter(value -> value != null && !value.isBlank())
        .map(String::trim)
        .distinct()
        .reduce((left, right) -> left + "," + right)
        .orElse(null);
  }

  @Override
  public List<String> convertToEntityAttribute(String value) {
    if (value == null || value.isBlank()) {
      return List.of();
    }

    return Arrays.stream(value.split("[;,]"))
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .distinct()
        .toList();
  }
}
