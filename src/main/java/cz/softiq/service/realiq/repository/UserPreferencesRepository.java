package cz.softiq.service.realiq.repository;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.UserPreferencesEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferencesRepository extends JpaRepository<UserPreferencesEntity, Long> {
  Optional<UserPreferencesEntity> findByClient(CoreClientEntity client);
}
