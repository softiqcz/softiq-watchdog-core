package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import java.time.LocalDate;
import java.util.List;

public interface IWatchDogPersistenceService extends IBeanProvider, IBaseService {

  default List<WatchDogEntity> getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum frequency) {
    return getWatchDogPersistenceService().getAllEnabledWatchDogs(frequency);
  }

  default List<WatchDogEntity> getAllNewWatchDogs() {
    return getWatchDogPersistenceService().getAllEnabledWatchDogs(Boolean.TRUE);
  }

  default List<WatchDogEntity> getAllWatchDogsToReset() {
    return getWatchDogPersistenceService().getAllWatchDogsToReset();
  }

  default void saveWatchDog(WatchDogEntity watchDog) {
    getWatchDogPersistenceService().saveWatchDog(watchDog);
  }

  default void setFirstSyncSuccess(WatchDogEntity watchDog) {
    getWatchDogPersistenceService().setFirstSyncSuccess(watchDog);
  }

  default List<WatchDogEntity> getWatchDogsByCoreClient(
      CoreClientEntity coreClient, TypeEnum watchDogType) {
    return getWatchDogPersistenceService()
        .getWatchDogsByCoreClientAndType(coreClient, watchDogType);
  }

  default WatchDogEntity getWatchDogById(Integer watchDogId) {
    return getWatchDogPersistenceService().getWatchDogById(watchDogId);
  }

  default List<WatchDogEntity> getWatchDogsByRealiqId(Integer realiqId) {
    return getWatchDogPersistenceService().getWatchDogsByRealiqId(realiqId);
  }

  default List<WatchDogEntity> getWatchDogChildrenByParentId(Integer parentWatchDogId) {
    return getWatchDogPersistenceService().getWatchDogChildren(parentWatchDogId);
  }

  default void saveResultEdit(
      WatchDogEstatesEntity estate, Boolean isFavorite, Boolean isHiddenFromClient) {
    getWatchDogPersistenceService().editResult(estate, isFavorite, isHiddenFromClient);
  }

  default void deleteWatchDog(WatchDogEntity watchDog) {
    getWatchDogPersistenceService().deleteWatchDog(watchDog);
    getWatchDogPersistenceService().deleteWatchDogChildren(watchDog);
  }

  default List<WatchDogEstatesEntity> getWatchDogEstateEntities(WatchDogEntity watchDog) {
    return getWatchDogPersistenceService().getWatchDogEstates(watchDog);
  }

  default List<WatchDogEstatesEntity> getWatchDogEstateEntitiesBetween(
      WatchDogEntity watchDog, LocalDate start, LocalDate end) {
    return getWatchDogPersistenceService().getWatchDogEstates(watchDog, start, end);
  }

  default WatchDogEstatesEntity getWatchDogEstateEntity(Integer id) {
    return getWatchDogPersistenceService().getWatchDogEstate(id);
  }

  default List<WatchDogEstatesEntity> getWatchDogEstateEntities(
      int countOfResults, WatchDogEntity watchDog) {
    return getWatchDogPersistenceService().getLatestWatchDogEstates(countOfResults, watchDog);
  }

  private WatchDogPersistenceService getWatchDogPersistenceService() {
    return getBean(WatchDogPersistenceService.class);
  }
}
