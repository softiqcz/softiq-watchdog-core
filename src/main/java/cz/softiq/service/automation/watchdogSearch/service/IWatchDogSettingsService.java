package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.Map;

public interface IWatchDogSettingsService extends IBeanProvider, IBaseService {
  default String setValuesToWatchDogAndSave(Map<String, Object> request, WatchDogEntity watchDog) {
    return getBean(WatchDogSettingsService.class).setValuesToWatchDogAndSave(request, watchDog);
  }
}
