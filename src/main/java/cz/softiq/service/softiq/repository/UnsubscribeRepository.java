package cz.softiq.service.softiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.softiq.entity.UnsubscribeEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UnsubscribeRepository
    extends JpaRepository<UnsubscribeEntity, Integer>, IBaseRepository {
  Optional<UnsubscribeEntity> findByClientEmail(String clientEmail);

  boolean existsByClientEmail(String email);
}
