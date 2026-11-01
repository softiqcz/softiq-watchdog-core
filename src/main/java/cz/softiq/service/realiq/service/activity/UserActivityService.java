package cz.softiq.service.realiq.service.activity;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.records.UserActivityItem;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class UserActivityService {
  private final UserActivityComponent userActivityComponent;

  List<UserActivityItem> getActivities(CoreClientEntity client) {
    return userActivityComponent.getActivities(client);
  }

  void deleteActivities(CoreClientEntity client) {
    userActivityComponent.deleteActivities(client);
  }

  void recordTransactionReminder(
      CoreClientEntity client,
      ClientEstate estate,
      Transaction transaction,
      String period,
      String subject,
      String htmlBody) {
    userActivityComponent.recordTransactionReminder(
        client, estate, transaction, period, subject, htmlBody);
  }

  void ensureTransactionReminderAllowed(
      CoreClientEntity client, Long transactionId, String period) {
    userActivityComponent.ensureTransactionReminderAllowed(client, transactionId, period);
  }

  void recordBankStatementImport(
      CoreClientEntity client,
      ClientEstate estate,
      String fileName,
      String format,
      int transactionCount,
      List<String> periods) {
    userActivityComponent.recordBankStatementImport(
        client, estate, fileName, format, transactionCount, periods);
  }

  Map<String, Object> extractData(Map<String, Object> requestJson) {
    return userActivityComponent.extractData(requestJson);
  }
}
