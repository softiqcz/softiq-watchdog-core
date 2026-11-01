package cz.softiq.service.softiq.service.reporting;

import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.email.CoreReportingEntity;
import cz.softiq.core.model.TestReport;
import cz.softiq.service.softiq.model.enums.ReportType;
import cz.softiq.service.softiq.model.projection.ReportEmailProjection;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.builder.WatchDogReportBuilder;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
class ReportingService {
  private final ReportingPersistenceComponent persistenceComponent;

  @Transactional
  void saveEmail(CoreReportingEntity reportingEntity) {
    persistenceComponent.saveEmail(reportingEntity);
  }

  Iterable<ReportEmailProjection> getAllSentReports() {
    return persistenceComponent.getAllSentReports();
  }

  String getReportBody(Map<String, Object> request) {
    return persistenceComponent.getReportBody(request);
  }

  @Transactional
  void saveTestReport(TestReport testReport) {
    persistenceComponent.saveTestReport(testReport);
  }

  Iterable<TestReport> getAllTestReports() {
    return persistenceComponent.getAllTestReports();
  }

  @Transactional
  long deleteOldTestReports(int days) {
    return persistenceComponent.deleteOldTestReports(days);
  }

  @Transactional
  void deleteAllLogs() {
    persistenceComponent.deleteAllLogs();
  }

  @Transactional
  void deleteAllReports(ReportType type) {
    persistenceComponent.deleteAllReports(type);
  }

  Iterable<CoreLogEntity> getAllLogs() {
    return persistenceComponent.getAllLogs();
  }

  String handleReporting(WatchDogReportBuilder report, WatchDogEntity watchDog) {
    return persistenceComponent.handleReporting(report, watchDog);
  }
}
