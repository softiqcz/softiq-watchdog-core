package cz.softiq.service.softiq.service.reporting;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.email.CoreReportingEntity;
import cz.softiq.core.model.TestReport;
import cz.softiq.service.softiq.model.enums.ReportType;
import cz.softiq.service.softiq.model.projection.ReportEmailProjection;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.builder.WatchDogReportBuilder;
import java.util.Map;

public interface IReportingService extends IBeanProvider, IBaseService {

  default void saveEmail(CoreReportingEntity reportingEntity) {
    getReportingService().saveEmail(reportingEntity);
  }

  default Iterable<ReportEmailProjection> getAllSentReports() {
    return getReportingService().getAllSentReports();
  }

  default String getReportBody(Map<String, Object> request) {
    return getReportingService().getReportBody(request);
  }

  default void saveTestReport(TestReport testReport) {
    getReportingService().saveTestReport(testReport);
  }

  default Iterable<TestReport> getAllTestReports() {
    return getReportingService().getAllTestReports();
  }

  default long deleteOldTestReports(int days) {
    return getReportingService().deleteOldTestReports(days);
  }

  default void deleteAllLogs() {
    getReportingService().deleteAllLogs();
  }

  default void deleteAllReports(ReportType type) {
    getReportingService().deleteAllReports(type);
  }

  default Iterable<CoreLogEntity> getAllLogs() {
    return getReportingService().getAllLogs();
  }

  default String handleReporting(WatchDogReportBuilder report, WatchDogEntity watchDog) {
    return getReportingService().handleReporting(report, watchDog);
  }

  private ReportingService getReportingService() {
    return getBean(ReportingService.class);
  }
}
