package cz.softiq.service.realiq.service.email;

import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.PortfolioRecordEntity;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.records.TransactionReminderEmail;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RealiqEmailService {
  private final RealiqEmailComponent realiqEmailComponent;

  TransactionReminderEmail transactionReminder(
      ClientEstate estate,
      Transaction transaction,
      String period,
      String ownerEmail,
      String ownerName) {
    return realiqEmailComponent.transactionReminder(
        estate, transaction, period, ownerEmail, ownerName);
  }

  void portfolioRecordReminder(PortfolioRecordEntity record, LocalDate occurrenceDate) {
    realiqEmailComponent.portfolioRecordReminder(record, occurrenceDate);
  }
}
