package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.watchdog.entity.InvoiceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InvoiceRepository extends JpaRepository<InvoiceEntity, Integer>, IBaseRepository {
  InvoiceEntity findTop1ByOrderByInvoiceNumberDesc();
}
