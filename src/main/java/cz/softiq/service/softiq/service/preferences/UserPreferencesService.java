package cz.softiq.service.softiq.service.preferences;

import cz.softiq.core.clients.CoreClientEntity;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserPreferencesService {
  private final UserPreferencesComponent userPreferencesComponent;

  Map<String, Object> getOrCreatePreferences(CoreClientEntity client) {
    return userPreferencesComponent.getOrCreatePreferences(client);
  }

  Map<String, Object> updatePreferences(
      CoreClientEntity client, Map<String, Object> preferenceUpdates) {
    return userPreferencesComponent.updatePreferences(client, preferenceUpdates);
  }
}
