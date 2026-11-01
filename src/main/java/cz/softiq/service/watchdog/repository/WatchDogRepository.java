package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchDogRepository
    extends JpaRepository<WatchDogEntity, Integer>, IBaseRepository {
  boolean existsByClient(CoreClientEntity client);

  List<WatchDogEntity> getAllByClientIdAndIsVisible(Integer clientId, Boolean isVisible);

  List<WatchDogEntity> getAllByClientAndIsVisibleAndType(
      CoreClientEntity client, Boolean isVisible, TypeEnum type);

  List<WatchDogEntity> getAllByClientId(Integer clientId);

  List<WatchDogEntity> getAllByIsEnabled(Boolean isEnabled);

  List<WatchDogEntity> getAllByParentWatchDogId(Integer parentWatchDogId);

  List<WatchDogEntity> getAllByIsEnabledAndIsFirstSyncCompletedAndDoResetSearch(
      Boolean isEnabled, Boolean isFirstSyncCompleted, Boolean doResetSearch);

  List<WatchDogEntity>
      getAllByIsEnabledAndIsFirstSyncCompletedAndDoResetSearchAndEmailReportFrequency(
          Boolean isEnabled,
          Boolean isFirstSyncCompleted,
          Boolean doResetSearch,
          SettingsEnums.FrequencyEnum emailReportFrequency);

  List<WatchDogEntity> getAllByDoResetSearch(Boolean resetSearch);

  List<WatchDogEntity> findAllByEmailReportFrequency(
      SettingsEnums.FrequencyEnum emailReportFrequency);

  List<WatchDogEntity> findAllByParentWatchDogIdIsNotNull();

  List<WatchDogEntity> findAllByRealiqId(Integer realiqId);
}
