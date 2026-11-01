package cz.softiq.service.softiq.service.preferences;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import java.util.Map;

public interface IUserPreferencesService extends IBeanProvider, IBaseService {

  private UserPreferencesService getUserPreferencesService() {
    return getBean(UserPreferencesService.class);
  }

  default Map<String, Object> getOrCreatePreferences(CoreClientEntity client) {
    return getUserPreferencesService().getOrCreatePreferences(client);
  }

  default Map<String, Object> updatePreferences(
      CoreClientEntity client, Map<String, Object> preferenceUpdates) {
    return getUserPreferencesService().updatePreferences(client, preferenceUpdates);
  }
}
