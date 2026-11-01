package cz.softiq.service.softiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.model.TestReport;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface TestReportRepository extends JpaRepository<TestReport, Integer>, IBaseRepository {
  @Transactional
  long deleteByCreatedAtBefore(LocalDateTime createdAt);
}
