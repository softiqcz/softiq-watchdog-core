package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchDogPersistenceService {
  private final WatchDogManagementComponent managementComponent;
  private final WatchDogPersistenceComponent persistenceComponent;

  List<WatchDogEntity> getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum frequency) {
    return persistenceComponent.getAllEnabledWatchDogs(frequency);
  }

  List<WatchDogEntity> getAllEnabledWatchDogs(Boolean isNewRun) {
    return persistenceComponent.getAllEnabledWatchDogs(isNewRun);
  }

  List<WatchDogEntity> getAllWatchDogsToReset() {
    return persistenceComponent.getAllWatchDogsToReset();
  }

  void saveWatchDog(WatchDogEntity watchDog) {
    persistenceComponent.saveWatchDog(watchDog);
  }

  void setFirstSyncSuccess(WatchDogEntity watchDog) {
    persistenceComponent.setFirstSyncSuccess(watchDog);
  }

  List<WatchDogEntity> getWatchDogsByCoreClientAndType(
      CoreClientEntity coreClient, TypeEnum watchDogType) {
    return persistenceComponent.getWatchDogsByCoreClientAndType(coreClient, watchDogType);
  }

  WatchDogEntity getWatchDogById(Integer watchDogId) {
    return managementComponent.getWatchDog(watchDogId);
  }

  List<WatchDogEntity> getWatchDogsByRealiqId(Integer realiqId) {
    return managementComponent.getAllWatchDogsByRealiqId(realiqId);
  }

  List<WatchDogEntity> getWatchDogChildren(Integer parentWatchDogId) {
    return persistenceComponent.getWatchDogChildren(parentWatchDogId);
  }

  void editResult(WatchDogEstatesEntity estate, Boolean isFavorite, Boolean isHiddenFromClient) {
    persistenceComponent.editResult(estate, isFavorite, isHiddenFromClient);
  }

  void deleteWatchDog(WatchDogEntity watchDog) {
    persistenceComponent.deleteWatchDog(watchDog);
  }

  void deleteWatchDogChildren(WatchDogEntity watchDog) {
    persistenceComponent.deleteWatchDogChildren(watchDog);
  }

  List<WatchDogEstatesEntity> getWatchDogEstates(WatchDogEntity watchDog) {
    return persistenceComponent.getWatchDogEstates(watchDog);
  }

  List<WatchDogEstatesEntity> getWatchDogEstates(
      WatchDogEntity watchDog, LocalDate start, LocalDate end) {
    return persistenceComponent.getWatchDogEstates(watchDog, start, end);
  }

  WatchDogEstatesEntity getWatchDogEstate(Integer id) {
    return persistenceComponent.getWatchDogEstate(id);
  }

  List<WatchDogEstatesEntity> getLatestWatchDogEstates(
      int countOfResults, WatchDogEntity watchDog) {
    return persistenceComponent.getLatestWatchDogEstates(countOfResults, watchDog);
  }
}
