package cz.softiq.service.realiq.service.portfolio;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.PortfolioEstateMonthlySummary;
import cz.softiq.service.realiq.model.PortfolioEstatesSummary;
import cz.softiq.service.realiq.model.PortfolioMonthlySummary;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.TransactionType;
import cz.softiq.service.realiq.repository.ClientEstateRepository;
import cz.softiq.service.realiq.repository.TransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PortfolioAnalysisComponent {

  private final TransactionRepository transactionRepository;
  private final ClientEstateRepository clientEstateRepository;

  public PortfolioEstatesSummary analyzeMonthlyTransactionsByEstate(
      CoreClientEntity client, LocalDate monthDate) {
    LocalDate monthStart = monthDate.withDayOfMonth(1);
    LocalDate monthEnd = monthStart.plusMonths(1).minusDays(1);

    Date periodStart = Date.from(monthStart.atStartOfDay(ZoneId.systemDefault()).toInstant());
    Date periodEnd =
        Date.from(monthEnd.atTime(23, 59, 59).atZone(ZoneId.systemDefault()).toInstant());

    List<ClientEstate> estates = clientEstateRepository.findAllByClient(client);

    PortfolioEstatesSummary estatesSummary = new PortfolioEstatesSummary();
    List<PortfolioEstateMonthlySummary> estateSummaries = new ArrayList<>();

    // Totals across all estates
    BigDecimal totalVerifiedIncome = BigDecimal.ZERO;
    BigDecimal totalVerifiedExpense = BigDecimal.ZERO;
    BigDecimal totalUnverifiedIncome = BigDecimal.ZERO;
    BigDecimal totalUnverifiedExpense = BigDecimal.ZERO;
    int totalVerifiedCount = 0;
    int totalUnverifiedCount = 0;

    for (ClientEstate estate : estates) {
      List<Transaction> transactions =
          transactionRepository.findActiveTransactionsByEstatesAndPeriod(
              List.of(estate), periodStart, periodEnd);

      // Include all estates, even with no transactions
      PortfolioEstateMonthlySummary estateSummary = new PortfolioEstateMonthlySummary();
      estateSummary.setEstateName(estate.getName());
      estateSummary.setEstateAddress(estate.getAddress());

      BigDecimal verifiedIncome = BigDecimal.ZERO;
      BigDecimal verifiedExpense = BigDecimal.ZERO;
      BigDecimal unverifiedIncome = BigDecimal.ZERO;
      BigDecimal unverifiedExpense = BigDecimal.ZERO;

      int verifiedCount = 0;
      int unverifiedCount = 0;

      for (Transaction transaction : transactions) {
        boolean isVerified = isTransactionVerified(transaction, monthStart);
        BigDecimal amount =
            transaction.getAmount() != null ? transaction.getAmount() : BigDecimal.ZERO;

        if (isVerified) {
          verifiedCount++;
          if (transaction.getTransactionType() == TransactionType.INCOME) {
            verifiedIncome = verifiedIncome.add(amount);
          } else if (transaction.getTransactionType() == TransactionType.EXPENSE) {
            verifiedExpense = verifiedExpense.add(amount);
          }
        } else {
          unverifiedCount++;
          if (transaction.getTransactionType() == TransactionType.INCOME) {
            unverifiedIncome = unverifiedIncome.add(amount);
          } else if (transaction.getTransactionType() == TransactionType.EXPENSE) {
            unverifiedExpense = unverifiedExpense.add(amount);
          }
        }
      }

      // Estate verified
      estateSummary.setVerifiedTransactionCount(verifiedCount);
      estateSummary.setVerifiedIncomeTotal(verifiedIncome);
      estateSummary.setVerifiedExpenseTotal(verifiedExpense);
      estateSummary.setVerifiedNetIncome(verifiedIncome.subtract(verifiedExpense));

      // Estate unverified
      estateSummary.setUnverifiedTransactionCount(unverifiedCount);
      estateSummary.setUnverifiedIncomeTotal(unverifiedIncome);
      estateSummary.setUnverifiedExpenseTotal(unverifiedExpense);
      estateSummary.setUnverifiedNetIncome(unverifiedIncome.subtract(unverifiedExpense));

      // Estate total
      estateSummary.setTotalTransactionCount(verifiedCount + unverifiedCount);
      estateSummary.setTotalIncomeTotal(verifiedIncome.add(unverifiedIncome));
      estateSummary.setTotalExpenseTotal(verifiedExpense.add(unverifiedExpense));
      estateSummary.setTotalNetIncome(
          estateSummary.getTotalIncomeTotal().subtract(estateSummary.getTotalExpenseTotal()));

      estateSummaries.add(estateSummary);

      // Add to totals
      totalVerifiedIncome = totalVerifiedIncome.add(verifiedIncome);
      totalVerifiedExpense = totalVerifiedExpense.add(verifiedExpense);
      totalUnverifiedIncome = totalUnverifiedIncome.add(unverifiedIncome);
      totalUnverifiedExpense = totalUnverifiedExpense.add(unverifiedExpense);
      totalVerifiedCount += verifiedCount;
      totalUnverifiedCount += unverifiedCount;
    }

    estatesSummary.setEstatesSummaries(estateSummaries);

    // Create total summary
    PortfolioMonthlySummary totalSummary = new PortfolioMonthlySummary();
    totalSummary.setEstateCount(estates.size());

    totalSummary.setVerifiedTransactionCount(totalVerifiedCount);
    totalSummary.setVerifiedIncomeTotal(totalVerifiedIncome);
    totalSummary.setVerifiedExpenseTotal(totalVerifiedExpense);
    totalSummary.setVerifiedNetIncome(totalVerifiedIncome.subtract(totalVerifiedExpense));

    totalSummary.setUnverifiedTransactionCount(totalUnverifiedCount);
    totalSummary.setUnverifiedIncomeTotal(totalUnverifiedIncome);
    totalSummary.setUnverifiedExpenseTotal(totalUnverifiedExpense);
    totalSummary.setUnverifiedNetIncome(totalUnverifiedIncome.subtract(totalUnverifiedExpense));

    totalSummary.setTotalTransactionCount(totalVerifiedCount + totalUnverifiedCount);
    totalSummary.setTotalIncomeTotal(totalVerifiedIncome.add(totalUnverifiedIncome));
    totalSummary.setTotalExpenseTotal(totalVerifiedExpense.add(totalUnverifiedExpense));
    totalSummary.setTotalNetIncome(
        totalSummary.getTotalIncomeTotal().subtract(totalSummary.getTotalExpenseTotal()));

    estatesSummary.setTotalSummary(totalSummary);

    return estatesSummary;
  }

  public PortfolioMonthlySummary analyzeMonthlyTransactions(
      CoreClientEntity client, LocalDate monthDate) {
    PortfolioEstatesSummary estatesSummary = analyzeMonthlyTransactionsByEstate(client, monthDate);
    return estatesSummary.getTotalSummary();
  }

  private boolean isTransactionVerified(Transaction transaction, LocalDate month) {
    if (transaction.getVerification() == null) {
      return false;
    }

    List<String> verifiedList = transaction.getVerification().getListOfVerifiedTransactions();
    return verifiedList != null
        && verifiedList.contains(month.format(DateTimeFormatter.ofPattern("MM yyyy")));
  }
}
