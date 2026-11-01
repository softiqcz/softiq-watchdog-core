package cz.softiq.service.realiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClientEstateRepository extends JpaRepository<ClientEstate, Long>, IBaseRepository {
  boolean existsByClient(CoreClientEntity client);

  List<ClientEstate> findByClient(CoreClientEntity client);

  List<ClientEstate> findAllByClient(CoreClientEntity client);

  long countByClient(CoreClientEntity client);

  @org.springframework.data.jpa.repository.Query("SELECT DISTINCT e.client FROM ClientEstate e")
  List<CoreClientEntity> findAllClientsWithEstates();
}
