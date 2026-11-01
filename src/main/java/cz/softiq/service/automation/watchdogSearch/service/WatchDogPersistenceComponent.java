package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.service.watchdog.model.enums.MembershipEnum.BASIC;
import static java.lang.Boolean.TRUE;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.entity.WatchDogOldClientEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import cz.softiq.service.watchdog.repository.WatchDogMembershipRepository;
import cz.softiq.service.watchdog.repository.WatchDogOldClientRepository;
import cz.softiq.service.watchdog.repository.WatchDogRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WatchDogPersistenceComponent {
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final WatchDogRepository watchDogRepository;
  private final WatchDogEstatesRepository watchDogEstatesRepository;
  private final WatchDogMembershipRepository watchDogMembershipRepository;
  private final WatchDogOldClientRepository watchDogOldClientRepository;

  public List<WatchDogEntity> getAllEnabledWatchDogs(SettingsEnums.FrequencyEnum frequency) {
    return watchDogRepository
        .getAllByIsEnabledAndIsFirstSyncCompletedAndDoResetSearchAndEmailReportFrequency(
            TRUE, TRUE, Boolean.FALSE, frequency);
  }

  public List<WatchDogEntity> getAllEnabledWatchDogs(Boolean isNewRun) {
    List<WatchDogEntity> watchDogs = new ArrayList<>();
    watchDogs.addAll(getAllEnabledWatchDogs(isNewRun, MembershipEnum.PREMIUM));
    watchDogs.addAll(getAllEnabledWatchDogs(isNewRun, MembershipEnum.STANDARD));
    watchDogs.addAll(getAllEnabledWatchDogs(isNewRun, BASIC));
    if (isNewRun) {
      watchDogs.addAll(getAllEnabledWatchDogs(isNewRun, MembershipEnum.PROFESSIONAL));
    }
    return watchDogs;
  }

  public List<WatchDogEntity> getAllWatchDogsToReset() {
    return watchDogRepository.getAllByDoResetSearch(true);
  }

  public void setFirstSyncSuccess(WatchDogEntity watchDog) {
    if (!watchDog.getIsFirstSyncCompleted() || watchDog.getDoResetSearch()) {
      watchDog.setIsFirstSyncCompleted(true);
      watchDog.setDoResetSearch(false);
      saveWatchDog(watchDog);
    }
  }

  public void saveWatchDog(WatchDogEntity watchDog) {
    watchDogRepository.save(watchDog);
  }

  public List<WatchDogEntity> getWatchDogsByCoreClientAndType(
      CoreClientEntity coreClient, TypeEnum watchDogType) {
    return watchDogRepository.getAllByClientAndIsVisibleAndType(
        coreClient, watchDogType == TypeEnum.AUTOMATION_WATCHDOG, watchDogType);
  }

  public List<WatchDogEntity> getWatchDogChildren(Integer parentWatchDogId) {
    return watchDogRepository.getAllByParentWatchDogId(parentWatchDogId);
  }

  public void editResult(
      WatchDogEstatesEntity estate, Boolean isFavorite, Boolean isHiddenFromClient) {
    estate.setIsFavorite(isFavorite);
    estate.setIsHiddenFromClientByUser(isHiddenFromClient);
    watchDogEstatesRepository.save(estate);
  }

  public List<WatchDogOldClientEntity> getOldClients() {
    return watchDogOldClientRepository.findAll();
  }

  public List<WatchDogMembershipEntity> getActiveMemberships() {
    return watchDogMembershipRepository.findAllByIsActive(true);
  }

  public void saveNewWatchDog(WatchDogEntity watchDog) {
    CoreClientEntity client = watchDogManagementComponent.getClient();
    List<WatchDogEntity> watchDogs =
        getWatchDogsByCoreClientAndType(client, TypeEnum.AUTOMATION_WATCHDOG);
    long enabledCount =
        watchDogs.stream().filter(entity -> Boolean.TRUE.equals(entity.getIsEnabled())).count();
    boolean canCreate =
        enabledCount < watchDogManagementComponent.getMaximumSizeForMembership(client);
    if (watchDog.getId() != null || canCreate) {
      watchDogManagementComponent.setWatchDogFields(watchDog);
      return;
    }
    throw new SoftiqException("Nelze vytvořit nový hlídací pes");
  }

  public void deleteWatchDog(WatchDogEntity watchDog) {
    if (watchDog == null) {
      throw new SoftiqException("Bohužel se nepovedlo smazat hlídacího psa.");
    }
    watchDogManagementComponent.deleteWatchDog(watchDog);
  }

  public void deleteWatchDogChildren(WatchDogEntity watchDog) {
    getWatchDogChildren(watchDog.getId()).forEach(this::deleteWatchDog);
  }

  public List<WatchDogEstatesEntity> getWatchDogEstates(WatchDogEntity watchDog) {
    return watchDogEstatesRepository.findWatchDogEstatesEntitiesToShow(watchDog);
  }

  public List<WatchDogEstatesEntity> getWatchDogEstates(
      WatchDogEntity watchDog, LocalDate start, LocalDate end) {
    return watchDogEstatesRepository.findWatchDogEstatesEntitiesToShowBetween(watchDog, start, end);
  }

  public WatchDogEstatesEntity getWatchDogEstate(Integer id) {
    return watchDogEstatesRepository.findById(id).orElse(null);
  }

  public List<WatchDogEstatesEntity> getLatestWatchDogEstates(
      int countOfResults, WatchDogEntity watchDog) {
    var pageable = PageRequest.of(0, countOfResults, Sort.by(Sort.Order.desc("id")));
    return watchDogEstatesRepository.findLatestWatchDogEstatesEntitiesByWatchdogEntity(
        watchDog, pageable);
  }

  private List<WatchDogEntity> getAllEnabledWatchDogs(Boolean isNewRun, MembershipEnum membership) {
    List<WatchDogEntity> allWatchDogs =
        watchDogRepository.getAllByIsEnabledAndIsFirstSyncCompletedAndDoResetSearch(
            true, !isNewRun, false);
    List<WatchDogMembershipEntity> memberships =
        watchDogMembershipRepository.findByIsActiveAndMembership(true, membership);
    return allWatchDogs.stream()
        .filter(
            watchDog ->
                memberships.stream()
                    .anyMatch(active -> active.getClient().equals(watchDog.getClient())))
        .toList();
  }
}
