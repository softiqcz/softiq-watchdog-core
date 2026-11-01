package cz.softiq.service.realiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.TaxReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxReportRepository extends JpaRepository<TaxReport, Long>, IBaseRepository {
  List<TaxReport> findTaxReportByEstate(ClientEstate estate);
}
