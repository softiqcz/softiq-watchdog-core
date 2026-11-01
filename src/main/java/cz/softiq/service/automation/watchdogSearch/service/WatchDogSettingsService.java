package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class WatchDogSettingsService {
  private final WatchDogSettingsComponent settingsComponent;

  String setValuesToWatchDogAndSave(Map<String, Object> request, WatchDogEntity watchDog) {
    return settingsComponent.setValuesToWatchDogAndSave(request, watchDog);
  }
}
