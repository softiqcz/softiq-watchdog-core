package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.email.CoreReportingEntity;
import cz.softiq.service.softiq.model.projection.ReportEmailProjection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportingRepository
    extends JpaRepository<CoreReportingEntity, Integer>, IBaseRepository {

  List<ReportEmailProjection> findAllProjectedByOrderByCreatedDateTimeDescIdDesc();

  @Query("select report.body from CoreReportingEntity report where report.id = :id")
  Optional<String> findBodyById(@Param("id") Integer id);

  void deleteAllByEmailTo(String email);
}
