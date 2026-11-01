package cz.softiq.service.softiq.service.preferences;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.realiq.model.entities.UserPreferencesEntity;
import cz.softiq.service.realiq.repository.UserPreferencesRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UserPreferencesComponent {
  private static final int MAX_PREFERENCES_LENGTH = 32_000;
  private static final Map<String, List<String>> DEFAULT_HIDDEN_SUGGESTIONS = Map.of();
  private static final List<String> DEFAULT_HIDDEN_SECTIONS = List.of();
  private static final List<String> HINT_WINDOW_KEYS =
      List.of(
          "portfolioOverview",
          "portfolioPerformance",
          "watchdogOverview",
          "watchdogResults",
          "watchdogPriceDevelopment",
          "analyticsViewTip",
          "watchdogFavoritesTip",
          "portfolioPredictionTip",
          "transactionImportTip",
          "transactionHistoryImportTip");

  private final UserPreferencesRepository userPreferencesRepository;

  @Transactional
  public Map<String, Object> getOrCreatePreferences(CoreClientEntity client) {
    UserPreferencesEntity entity =
        userPreferencesRepository
            .findByClient(client)
            .orElseGet(
                () -> {
                  UserPreferencesEntity created = new UserPreferencesEntity();
                  created.setClient(client);
                  created.setPreferences(defaultPreferences());
                  return userPreferencesRepository.save(created);
                });

    Map<String, Object> normalized = normalizePreferences(entity.getPreferences());
    if (!normalized.equals(entity.getPreferences())) {
      entity.setPreferences(normalized);
      userPreferencesRepository.save(entity);
    }
    return new LinkedHashMap<>(normalized);
  }

  @Transactional
  public Map<String, Object> updatePreferences(
      CoreClientEntity client, Map<String, Object> preferenceUpdates) {
    if (preferenceUpdates == null) {
      throw new SoftiqException("Missing user preferences");
    }
    if (preferenceUpdates.toString().length() > MAX_PREFERENCES_LENGTH) {
      throw new SoftiqException("User preferences are too large");
    }

    UserPreferencesEntity entity =
        userPreferencesRepository
            .findByClient(client)
            .orElseGet(
                () -> {
                  UserPreferencesEntity created = new UserPreferencesEntity();
                  created.setClient(client);
                  created.setPreferences(defaultPreferences());
                  return created;
                });
    Map<String, Object> merged = new LinkedHashMap<>(entity.getPreferences());
    merged.putAll(preferenceUpdates);
    Map<String, Object> normalized = normalizePreferences(merged);
    entity.setPreferences(normalized);
    userPreferencesRepository.save(entity);
    return new LinkedHashMap<>(normalized);
  }

  private Map<String, Object> defaultPreferences() {
    Map<String, Object> defaults = new LinkedHashMap<>();
    defaults.put("hideSuggestedBox", DEFAULT_HIDDEN_SUGGESTIONS);
    defaults.put("mode", "light");
    defaults.put("hideSection", DEFAULT_HIDDEN_SECTIONS);
    defaults.put("hintWindows", defaultHintWindows());
    return defaults;
  }

  private Map<String, Object> normalizePreferences(Map<String, Object> preferences) {
    Map<String, Object> normalized = new LinkedHashMap<>();
    if (preferences != null) {
      normalized.putAll(preferences);
    }

    normalized.put(
        "hideSuggestedBox", normalizeEstateSuggestions(normalized.get("hideSuggestedBox")));
    normalized.put("hideSection", normalizeHiddenSections(normalized.get("hideSection")));
    normalized.put("mode", "dark".equals(normalized.get("mode")) ? "dark" : "light");
    normalized.put("hintWindows", normalizeHintWindows(normalized.get("hintWindows")));
    return normalized;
  }

  private Map<String, Boolean> defaultHintWindows() {
    Map<String, Boolean> defaults = new LinkedHashMap<>();
    HINT_WINDOW_KEYS.forEach(key -> defaults.put(key, false));
    defaults.put("analyticsViewTip", true);
    defaults.put("watchdogFavoritesTip", true);
    defaults.put("portfolioPredictionTip", true);
    defaults.put("transactionImportTip", true);
    defaults.put("transactionHistoryImportTip", true);
    return defaults;
  }

  private Map<String, Boolean> normalizeHintWindows(Object value) {
    Map<String, Boolean> normalized = defaultHintWindows();
    if (!(value instanceof Map<?, ?> values)) {
      return normalized;
    }

    HINT_WINDOW_KEYS.forEach(
        key -> {
          if (values.get(key) instanceof Boolean isOpen) {
            normalized.put(key, isOpen);
          }
        });
    return normalized;
  }

  private List<String> normalizeStringList(Object value) {
    if (!(value instanceof List<?> values)) {
      return new ArrayList<>();
    }

    return values.stream()
        .filter(String.class::isInstance)
        .map(String.class::cast)
        .map(String::trim)
        .filter(item -> !item.isBlank())
        .distinct()
        .limit(100)
        .toList();
  }

  private List<String> normalizeHiddenSections(Object value) {
    List<String> hiddenSections = normalizeStringList(value);
    if (hiddenSections.contains("watchdog") && hiddenSections.contains("portfolio")) {
      return new ArrayList<>();
    }
    return hiddenSections;
  }

  private Map<String, List<String>> normalizeEstateSuggestions(Object value) {
    if (!(value instanceof Map<?, ?> values)) {
      return new LinkedHashMap<>();
    }

    Map<String, List<String>> normalized = new LinkedHashMap<>();
    values.entrySet().stream()
        .filter(entry -> entry.getKey() instanceof String)
        .limit(100)
        .forEach(
            entry -> {
              String estateId = ((String) entry.getKey()).trim();
              if (estateId.matches("\\d+")) {
                List<String> suggestions = normalizeStringList(entry.getValue());
                if (!suggestions.isEmpty()) {
                  normalized.put(estateId, suggestions);
                }
              }
            });
    return normalized;
  }
}
