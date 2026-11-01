package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.watchdog.entity.WatchDogOldClientEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WatchDogOldClientRepository
    extends JpaRepository<WatchDogOldClientEntity, Integer>, IBaseRepository {
  List<WatchDogOldClientEntity> findByEmail(String email);
}
