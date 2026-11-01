package cz.softiq.service.realiq.service.email;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.PortfolioRecordEntity;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.records.TransactionReminderEmail;
import java.time.LocalDate;

public interface IRealiqEmailService extends IBeanProvider, IBaseService {

  private RealiqEmailService getRealiqEmailService() {
    return getBean(RealiqEmailService.class);
  }

  default TransactionReminderEmail transactionReminder(
      ClientEstate estate,
      Transaction transaction,
      String period,
      String ownerEmail,
      String ownerName) {
    return getRealiqEmailService()
        .transactionReminder(estate, transaction, period, ownerEmail, ownerName);
  }

  default void portfolioRecordReminder(PortfolioRecordEntity record, LocalDate occurrenceDate) {
    getRealiqEmailService().portfolioRecordReminder(record, occurrenceDate);
  }
}
