package cz.softiq.service.realiq.service.transaction;

import cz.softiq.service.realiq.model.*;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.Recurrence;
import cz.softiq.service.realiq.model.enums.TransactionType;
import cz.softiq.service.realiq.repository.TransactionRepository;
import cz.softiq.service.realiq.service.realiq.IRealiqService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionComponent implements IRealiqService {
  private static final DateTimeFormatter VERIFICATION_PERIOD_FORMAT =
      DateTimeFormatter.ofPattern("MM uuuu");

  private final TransactionRepository transactionRepository;

  public BigDecimal calculateTotalByType(
      List<Transaction> transactions, TransactionType type, Boolean isTaxDeductible, Integer year) {
    if (transactions == null || transactions.isEmpty()) return BigDecimal.ZERO;

    LocalDate yearStart = LocalDate.of(year, 1, 1);
    LocalDate yearEnd = LocalDate.of(year, 12, 31);

    return transactions.stream()
        .filter(t -> t.getTransactionType() == type)
        .filter(t -> t.isTaxDeductible() == isTaxDeductible)
        .map(t -> calculateTransactionTotalForPeriod(t, yearStart, yearEnd))
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private BigDecimal calculateTransactionTotal(Transaction t) {
    return calculateTransactionTotalForPeriod(t, null, null);
  }

  private BigDecimal calculateTransactionTotalForPeriod(
      Transaction t, LocalDate reportStart, LocalDate reportEnd) {
    if (t.getAmount() == null || t.getRecurrence() == null) return BigDecimal.ZERO;

    LocalDate start = convertToLocalDate(t.getDateFrom());
    LocalDate end = convertToLocalDate(t.getDateTo());

    if (reportStart != null && start.isBefore(reportStart)) start = reportStart;
    if (reportEnd != null && end.isAfter(reportEnd)) end = reportEnd;

    if (end.isBefore(start)) return BigDecimal.ZERO;

    long occurrences =
        switch (t.getRecurrence()) {
          case ONCE -> 1;
          case MONTHLY -> ChronoUnit.MONTHS.between(start, end) + 1;
          case YEARLY -> ChronoUnit.YEARS.between(start, end) + 1;
          default -> 1;
        };

    return t.getAmount().multiply(BigDecimal.valueOf(occurrences));
  }

  private LocalDate convertToLocalDate(Date date) {
    if (date == null) return LocalDate.now();
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  public void saveTransaction(Transaction transaction) {
    transactionRepository.save(transaction);
  }

  public void deleteTransaction(Transaction transaction) {
    transactionRepository.delete(transaction);
  }

  public void verifyPeriod(Transaction transaction, String period) {
    YearMonth parsedPeriod;
    try {
      parsedPeriod = YearMonth.parse(period, VERIFICATION_PERIOD_FORMAT);
    } catch (DateTimeException exception) {
      throw new IllegalArgumentException("Invalid transaction verification period", exception);
    }

    TransactionVerification currentVerification = transaction.getVerification();
    List<String> verifiedPeriods =
        currentVerification == null || currentVerification.getListOfVerifiedTransactions() == null
            ? new ArrayList<>()
            : new ArrayList<>(currentVerification.getListOfVerifiedTransactions());

    String normalizedPeriod = parsedPeriod.format(VERIFICATION_PERIOD_FORMAT);
    if (!verifiedPeriods.contains(normalizedPeriod)) {
      verifiedPeriods.add(normalizedPeriod);
      verifiedPeriods.sort(
          Comparator.comparing(value -> YearMonth.parse(value, VERIFICATION_PERIOD_FORMAT)));
    }
    TransactionVerification updatedVerification = new TransactionVerification();
    updatedVerification.setListOfVerifiedTransactions(verifiedPeriods);
    transaction.setVerification(updatedVerification);
  }

  public void verifyPeriodFromDate(Transaction transaction) {
    if (transaction.getDateFrom() == null) {
      return;
    }
    YearMonth period =
        YearMonth.from(
            transaction.getDateFrom().toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
    verifyPeriod(transaction, period.format(VERIFICATION_PERIOD_FORMAT));
  }

  public boolean bankImportExists(Long estateId, String bankImportId) {
    return bankImportId != null
        && !bankImportId.isBlank()
        && transactionRepository.existsByEstateIdAndBankImportId(estateId, bankImportId);
  }

  public void endPreviousImportedRecurringTransaction(Transaction incomingTransaction) {
    if (incomingTransaction.getBankImportSeriesId() == null
        || incomingTransaction.getBankImportSeriesId().isBlank()
        || incomingTransaction.getDateFrom() == null
        || incomingTransaction.getAmount() == null
        || incomingTransaction.getRecurrence() == Recurrence.ONCE) {
      return;
    }

    List<Transaction> candidates =
        transactionRepository.findAllByEstateIdAndBankImportSeriesIdOrderByDateFromDesc(
            incomingTransaction.getEstate().getId(), incomingTransaction.getBankImportSeriesId());
    if (candidates.isEmpty()
        && incomingTransaction.getName() != null
        && !incomingTransaction.getName().isBlank()) {
      candidates =
          transactionRepository
              .findAllByEstateIdAndNameIgnoreCaseAndTransactionTypeAndBankImportIdIsNotNullOrderByDateFromDesc(
                  incomingTransaction.getEstate().getId(),
                  incomingTransaction.getName(),
                  incomingTransaction.getTransactionType());
    }

    Transaction previousTransaction =
        candidates.stream()
            .filter(candidate -> candidate.getRecurrence() == incomingTransaction.getRecurrence())
            .filter(candidate -> candidate.getDateFrom() != null)
            .filter(candidate -> candidate.getDateFrom().before(incomingTransaction.getDateFrom()))
            .filter(
                candidate ->
                    candidate.getDateTo() == null
                        || !candidate.getDateTo().before(incomingTransaction.getDateFrom()))
            .findFirst()
            .orElse(null);
    if (previousTransaction == null) {
      return;
    }

    ZonedDateTime incomingStart =
        incomingTransaction.getDateFrom().toInstant().atZone(ZoneId.systemDefault());
    ZonedDateTime previousEnd =
        incomingTransaction.getRecurrence() == Recurrence.MONTHLY
            ? incomingStart.minusMonths(1)
            : incomingStart.minusYears(1);
    if (previousEnd.toInstant().isBefore(previousTransaction.getDateFrom().toInstant())) {
      previousEnd = previousTransaction.getDateFrom().toInstant().atZone(ZoneId.systemDefault());
    }
    previousTransaction.setDateTo(Date.from(previousEnd.toInstant()));
    previousTransaction.setBankImportSeriesId(incomingTransaction.getBankImportSeriesId());
    transactionRepository.save(previousTransaction);
  }

  public void endRecurringTransactionById(
      Transaction incomingTransaction, Long previousTransactionId) {
    if (previousTransactionId == null
        || incomingTransaction.getDateFrom() == null
        || incomingTransaction.getRecurrence() == Recurrence.ONCE) {
      return;
    }

    Transaction previousTransaction = getTransaction(previousTransactionId);
    if (!previousTransaction.getEstate().getId().equals(incomingTransaction.getEstate().getId())
        || previousTransaction.getTransactionType() != incomingTransaction.getTransactionType()
        || previousTransaction.getRecurrence() == null
        || previousTransaction.getRecurrence() == Recurrence.ONCE
        || previousTransaction.getDateFrom() == null
        || !previousTransaction.getDateFrom().before(incomingTransaction.getDateFrom())
        || (previousTransaction.getDateTo() != null
            && previousTransaction.getDateTo().before(incomingTransaction.getDateFrom()))) {
      return;
    }

    ZonedDateTime incomingStart =
        incomingTransaction.getDateFrom().toInstant().atZone(ZoneId.systemDefault());
    ZonedDateTime previousEnd = incomingStart.minusMonths(1);
    if (previousEnd.toInstant().isBefore(previousTransaction.getDateFrom().toInstant())) {
      previousEnd = previousTransaction.getDateFrom().toInstant().atZone(ZoneId.systemDefault());
    }
    previousTransaction.setDateTo(Date.from(previousEnd.toInstant()));
    transactionRepository.save(previousTransaction);
  }

  public Transaction getTransaction(Long id) {
    return transactionRepository.findById(id).orElseThrow();
  }

  public Transaction convertToTransaction(Map<String, Object> transaction) {
    Transaction newTransaction;

    Integer id = (Integer) transaction.get("id");

    if (id != 0) {
      newTransaction = getTransaction(id.longValue());
    } else {
      newTransaction = new Transaction();
    }

    TransactionType transactionType =
        TransactionType.valueOf(transaction.get("transactionType").toString());
    Recurrence recurrence = Recurrence.valueOf(transaction.get("recurrence").toString());
    String name = (String) transaction.get("name");

    Boolean showInDashboard =
        transaction.get("showInDashboard") != null
            ? (Boolean) transaction.get("showInDashboard")
            : true;

    BigDecimal amount = new BigDecimal(transaction.get("amount").toString());
    int estateId = (Integer) transaction.get("estateId");
    String dateFromStr = (String) transaction.get("dateFrom");
    String dateToStr = (String) transaction.get("dateTo");

    if (dateFromStr != null && !dateFromStr.isBlank()) {
      Instant instantFrom = Instant.parse(dateFromStr);
      newTransaction.setDateFrom(Timestamp.from(instantFrom));
    }

    if (dateToStr != null && !dateToStr.isBlank()) {
      Instant instantTo = Instant.parse(dateToStr);
      newTransaction.setDateTo(Timestamp.from(instantTo));
    }
    newTransaction.setTransactionType(transactionType);
    newTransaction.setRecurrence(recurrence);
    newTransaction.setShowInDashboard(showInDashboard);
    newTransaction.setName(name);
    newTransaction.setAmount(amount);
    newTransaction.setEstate(getClientEstateBy((long) estateId));
    if (transaction.containsKey("bankImportId")) {
      Object bankImportId = transaction.get("bankImportId");
      newTransaction.setBankImportId(bankImportId != null ? bankImportId.toString() : null);
    }
    if (transaction.containsKey("bankImportSeriesId")) {
      Object bankImportSeriesId = transaction.get("bankImportSeriesId");
      newTransaction.setBankImportSeriesId(
          bankImportSeriesId != null ? bankImportSeriesId.toString() : null);
    }
    return newTransaction;
  }
}
