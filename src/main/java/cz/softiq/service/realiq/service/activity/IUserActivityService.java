package cz.softiq.service.realiq.service.activity;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.records.UserActivityItem;
import java.util.List;
import java.util.Map;

public interface IUserActivityService extends IBeanProvider, IBaseService {

  private UserActivityService getUserActivityService() {
    return getBean(UserActivityService.class);
  }

  default List<UserActivityItem> getActivities(CoreClientEntity client) {
    return getUserActivityService().getActivities(client);
  }

  default void deleteActivities(CoreClientEntity client) {
    getUserActivityService().deleteActivities(client);
  }

  default void recordTransactionReminder(
      CoreClientEntity client,
      ClientEstate estate,
      Transaction transaction,
      String period,
      String subject,
      String htmlBody) {
    getUserActivityService()
        .recordTransactionReminder(client, estate, transaction, period, subject, htmlBody);
  }

  default void ensureTransactionReminderAllowed(
      CoreClientEntity client, Long transactionId, String period) {
    getUserActivityService().ensureTransactionReminderAllowed(client, transactionId, period);
  }

  default void recordBankStatementImport(
      CoreClientEntity client,
      ClientEstate estate,
      String fileName,
      String format,
      int transactionCount,
      List<String> periods) {
    getUserActivityService()
        .recordBankStatementImport(client, estate, fileName, format, transactionCount, periods);
  }

  default Map<String, Object> extractData(Map<String, Object> requestJson) {
    return getUserActivityService().extractData(requestJson);
  }
}
