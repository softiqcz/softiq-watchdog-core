package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchDogMembershipRepository
    extends JpaRepository<WatchDogMembershipEntity, Integer>, IBaseRepository {
  List<WatchDogMembershipEntity> findByIsActiveAndClient(
      boolean isActive, CoreClientEntity coreClient);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<WatchDogMembershipEntity> findFirstByIsActiveAndClient(
      boolean isActive, CoreClientEntity coreClient);

  List<WatchDogMembershipEntity> findByIsActiveAndMembership(
      boolean isActive, MembershipEnum membership);

  List<WatchDogMembershipEntity> findAllByClient(CoreClientEntity coreClientEntity);

  WatchDogMembershipEntity findByUniqueHash(String uniqueHash);

  List<WatchDogMembershipEntity> findAllByMembership(MembershipEnum membership);

  List<WatchDogMembershipEntity> findAllByIsActive(boolean isActive);

  boolean existsByPromoCodeAndClient(PromoCodeEnum promoCode, CoreClientEntity client);

  @Query(
      "select count(distinct m.client.id) from WatchDogMembershipEntity m where m.promoCode = :promoCode")
  long countRedeemersByPromoCode(PromoCodeEnum promoCode);

  List<WatchDogMembershipEntity> getAllByIsActiveAndValidToBeforeAndMembershipIsNot(
      boolean isActive, LocalDateTime date, MembershipEnum membership);
}
