package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchDogEstatesRepository
    extends JpaRepository<WatchDogEstatesEntity, Integer>, IBaseRepository {

  @Query("SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity = :watchDog")
  List<WatchDogEstatesEntity> findAllWatchDogEstateEntities(
      @Param("watchDog") WatchDogEntity watchDog);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity.client = :coreClientEntity AND w.isReported = :isReported")
  List<WatchDogEstatesEntity> findWatchDogEstatesEntitiesByClientAndIsReported(
      @Param("coreClientEntity") CoreClientEntity coreClientEntity,
      @Param("isReported") Boolean isReported);

  void deleteAllByWatchdogEntity(WatchDogEntity watchDog);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity = :watchDog AND w.isReported = false AND w.isHiddenFromClientByAdmin = true AND w.isHiddenFromClientByUser = false")
  List<WatchDogEstatesEntity> findNewWatchDogEstates(@Param("watchDog") WatchDogEntity watchDog);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity = :watchDog AND w.isReported = true AND COALESCE(w.isHiddenFromClientByAdmin, false) = false AND COALESCE(w.isHiddenFromClientByUser, false) = false ORDER BY w.id DESC")
  List<WatchDogEstatesEntity> findWatchDogEstatesEntitiesToShow(
      @Param("watchDog") WatchDogEntity watchDog);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity = :watchDog AND w.isReported = true AND COALESCE(w.isHiddenFromClientByAdmin, false) = false AND COALESCE(w.isHiddenFromClientByUser, false) = false ORDER BY w.id DESC")
  List<WatchDogEstatesEntity> findLatestWatchDogEstatesEntitiesByWatchdogEntity(
      @Param("watchDog") WatchDogEntity watchDog, Pageable pageable);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity = :watchDog AND w.isReported = true AND COALESCE(w.isHiddenFromClientByAdmin, false) = false AND COALESCE(w.isHiddenFromClientByUser, false) = false AND w.createdAt >= :start AND w.createdAt < :end ORDER BY w.id DESC")
  List<WatchDogEstatesEntity> findWatchDogEstatesEntitiesToShowBetween(
      @Param("watchDog") WatchDogEntity watchDog,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity.id = :watchDogId AND w.isReported = true AND COALESCE(w.isHiddenFromClientByAdmin, false) = false AND COALESCE(w.isHiddenFromClientByUser, false) = false AND w.createdAt >= :start AND w.createdAt < :end")
  List<WatchDogEstatesEntity> findPublicWatchDogEstatesBetween(
      @Param("watchDogId") Integer watchDogId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  @Query(
      "SELECT w FROM WatchDogEstatesEntity w WHERE w.watchdogEntity.id = :watchDogId AND COALESCE(w.isHiddenFromClientByUser, false) = false AND (COALESCE(w.isReported, false) = false OR COALESCE(w.isHiddenFromClientByAdmin, false) = false) AND w.createdAt >= :start AND w.createdAt < :end")
  List<WatchDogEstatesEntity> findAdminWatchDogEstatesBetween(
      @Param("watchDogId") Integer watchDogId,
      @Param("start") LocalDate start,
      @Param("end") LocalDate end);

  List<WatchDogEstatesEntity> findByWatchdogEntityAndCreatedAtBefore(
      WatchDogEntity watchDog, LocalDate date);
}
