package cz.softiq.service.realiq.repository;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.UserActivityEntity;
import cz.softiq.service.realiq.model.enums.UserActivityType;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserActivityRepository extends JpaRepository<UserActivityEntity, Long> {
  List<UserActivityEntity> findTop50ByClientOrderByCreatedAtDesc(CoreClientEntity client);

  List<UserActivityEntity> findByClientAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
      CoreClientEntity client, UserActivityType type, LocalDateTime createdAfter);

  void deleteAllByClient(CoreClientEntity client);
}
