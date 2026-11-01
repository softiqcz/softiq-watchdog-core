package cz.softiq.service.softiq.service.reporting;

import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.email.CoreLogRepository;
import cz.softiq.core.email.CoreReportingEntity;
import cz.softiq.core.email.CoreReportingRepository;
import cz.softiq.core.model.TestReport;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogPersistenceService;
import cz.softiq.service.softiq.model.enums.ReportType;
import cz.softiq.service.softiq.model.projection.ReportEmailProjection;
import cz.softiq.service.softiq.repository.TestReportRepository;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.builder.WatchDogReportBuilder;
import cz.softiq.service.watchdog.repository.ReportingRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class ReportingPersistenceComponent implements IWatchDogPersistenceService {
  private final CoreReportingRepository reportingRepository;
  private final ReportingRepository reportReadRepository;
  private final TestReportRepository testReportRepository;
  private final CoreLogRepository logRepository;

  private static final String SCRAPER_SUBJECT = "Hlídací pes na nemovitosti";
  private static final String INFO_EMAIL = "info@softiq.cz";

  public void deleteAllReports(ReportType type) {
    if (type == null) {
      throw new SoftiqException("Type must not be null");
    }

    switch (type) {
      case ALL -> deleteAllReports();
      case SCRAPER -> deleteReportsBySubject(SCRAPER_SUBJECT);
      case INFO -> deleteReportsByRecipient(INFO_EMAIL);
      default -> throw new SoftiqException("Unknown type: " + type);
    }
  }

  public String handleReporting(WatchDogReportBuilder report, WatchDogEntity watchDog) {
    boolean isSuccess = report.getIsSuccess();
    if (isSuccess) {
      setFirstSyncSuccess(watchDog);
      return String.format(
          "%d %s %s  %s %s",
          watchDog.getId(),
          watchDog.getOfferType(),
          watchDog.getEstateType(),
          watchDog.getExactAddress(),
          watchDog.getClient().getClientName());
    } else {
      List<String> failures = new ArrayList<>();
      if (report.getDidBezrealitkyFail()) failures.add(report.getErrorMessageBezrealitky());
      if (report.getDidSrealityFail()) failures.add(report.getErrorMessageSreality());
      if (report.getDidBazosFail()) failures.add(report.getErrorMessageBazos());
      if (report.getDidIdnesFail()) failures.add(report.getErrorMessageIdnes());
      if (report.getDidRealitymatFail()) failures.add(report.getErrorMessageRealitymat());
      if (report.getDidCeskeRealityFail()) failures.add(report.getErrorMessageCeskeReality());
      String failMessage =
          failures.stream()
              .filter(message -> message != null && !message.isBlank())
              .collect(Collectors.joining("; "));
      if (failMessage.isBlank()) {
        failMessage = "Unknown provider error (no exception details were recorded)";
      }

      return String.format(
          "<p style=\"color: red; font-weight: bold;\">Watchdog search failed %d %s %s at %s</p>",
          watchDog.getId(),
          watchDog.getExactAddress(),
          watchDog.getClient().getClientName(),
          failMessage);
    }
  }

  public void saveEmail(CoreReportingEntity reportingEntity) {
    reportingRepository.save(reportingEntity);
  }

  public Iterable<ReportEmailProjection> getAllSentReports() {
    return reportReadRepository.findAllProjectedByOrderByCreatedDateTimeDescIdDesc();
  }

  public String getReportBody(Map<String, Object> request) {
    Object data = request.get("data");
    if (!(data instanceof Map<?, ?> payload)
        || !(payload.get("id") instanceof Number id)
        || id.doubleValue() != id.intValue()
        || id.intValue() <= 0) {
      throw new IllegalArgumentException("A positive report id is required");
    }
    return reportReadRepository
        .findBodyById(id.intValue())
        .orElseThrow(() -> new IllegalArgumentException("Report not found"));
  }

  public void saveTestReport(TestReport testReport) {
    testReportRepository.save(testReport);
  }

  public Iterable<TestReport> getAllTestReports() {
    return testReportRepository.findAll();
  }

  public long deleteOldTestReports(int days) {
    return testReportRepository.deleteByCreatedAtBefore(LocalDateTime.now().minusDays(days));
  }

  public void deleteAllLogs() {
    logRepository.deleteAll();
  }

  public void deleteAllReports() {
    reportingRepository.deleteAll();
  }

  public void deleteReportsBySubject(String subject) {
    reportingRepository.deleteAllBySubject(subject);
  }

  public void deleteReportsByRecipient(String email) {
    reportingRepository.deleteAllByEmailTo(email);
  }

  public Iterable<CoreLogEntity> getAllLogs() {
    return logRepository.findAll();
  }
}
