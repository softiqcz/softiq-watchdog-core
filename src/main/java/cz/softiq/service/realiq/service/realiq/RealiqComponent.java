package cz.softiq.service.realiq.service.realiq;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogPersistenceService;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.TaxReport;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.ActionEnum;
import cz.softiq.service.realiq.model.enums.EstateWatchDogResultType;
import cz.softiq.service.realiq.model.enums.TaxRate;
import cz.softiq.service.realiq.model.enums.TransactionType;
import cz.softiq.service.realiq.model.records.EstateAnalysis;
import cz.softiq.service.realiq.model.records.EstateWatchDogOffer;
import cz.softiq.service.realiq.service.activity.IUserActivityService;
import cz.softiq.service.realiq.service.email.IRealiqEmailService;
import cz.softiq.service.realiq.service.transaction.ITransactionService;
import cz.softiq.service.realiq.service.transaction.TransactionComponent;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class RealiqComponent
    implements ITransactionService,
        IWatchDogAccountService,
        IWatchDogPersistenceService,
        IRealiqEmailService,
        IUserActivityService {
  private final RealiqPersistenceComponent realiqPersistenceComponent;
  private final TransactionComponent transactionComponent;

  public List<ClientEstate> getAllUserEstate(CoreClientEntity client) {
    return realiqPersistenceComponent.getClientEstates(client);
  }

  @Transactional
  public List<TaxReport> generateReports(CoreClientEntity client) {
    var estateReports = getAllUserEstate(client);

    List<TaxReport> taxReports = new ArrayList<>();
    for (ClientEstate estate : estateReports) {
      taxReports.add(generateReport(estate));
    }

    return taxReports;
  }

  public List<EstateWatchDogOffer> getUserWatchDogsResults(List<WatchDogEntity> watchDogEntities) {
    List<EstateWatchDogOffer> results = new ArrayList<>();
    List<WatchDogEntity> sortedWatchDogs =
        watchDogEntities.stream().sorted(Comparator.comparing(WatchDogEntity::getId)).toList();
    int rentWatchDogIndex = 0;

    for (WatchDogEntity watchDogEntity : sortedWatchDogs) {
      EstateWatchDogResultType analysisType;
      if ("Prodej".equalsIgnoreCase(watchDogEntity.getOfferType())) {
        analysisType = EstateWatchDogResultType.SALE;
      } else {
        analysisType =
            rentWatchDogIndex++ == 0
                ? EstateWatchDogResultType.RENT_CURRENT
                : EstateWatchDogResultType.RENT_RENOVATED;
      }

      getWatchDogEstateEntities(watchDogEntity)
          .forEach(offer -> results.add(EstateWatchDogOffer.from(offer, analysisType)));
    }
    return results;
  }

  public EstateAnalysis getEstateAnalysis(List<WatchDogEntity> watchDogs) {
    List<Double> currentRent = new ArrayList<>();
    List<Double> renovatedRent = new ArrayList<>();
    List<Double> sale = new ArrayList<>();
    int rentIndex = 0;
    for (WatchDogEntity watchDog :
        watchDogs.stream().sorted(Comparator.comparing(WatchDogEntity::getId)).toList()) {
      List<Double> prices =
          "Prodej".equalsIgnoreCase(watchDog.getOfferType())
              ? sale
              : rentIndex++ == 0 ? currentRent : renovatedRent;
      // Hidden and unreported listings contribute only to aggregate analytics.
      // Client-facing listing endpoints keep their existing visibility filters.
      for (var offer : realiqPersistenceComponent.getOffersForAnalysis(watchDog)) {
        Double price = parseAnalysisPrice(offer.getPrice());
        if (price != null) prices.add(price);
      }
    }
    double rent = medianPrice(currentRent);
    return new EstateAnalysis(rent, Math.max(rent, medianPrice(renovatedRent)), medianPrice(sale));
  }

  private Double parseAnalysisPrice(String price) {
    if (price == null) return null;
    String numeric = price.replaceAll("[^\\d.]", "");
    if (numeric.isEmpty()) return null;
    try {
      double value = Double.parseDouble(numeric);
      return Double.isFinite(value) && value > 0 ? value : null;
    } catch (NumberFormatException ignored) {
      return null;
    }
  }

  private double medianPrice(List<Double> prices) {
    if (prices.isEmpty()) return 0;
    prices.sort(Double::compareTo);
    int middle = prices.size() / 2;
    return prices.size() % 2 == 1
        ? prices.get(middle)
        : Math.round((prices.get(middle - 1) + prices.get(middle)) / 2);
  }

  public List<TaxReport> getTaxReport(ClientEstate estate) {
    return realiqPersistenceComponent.getTaxReports(estate);
  }

  public TaxReport saveReport(boolean isNew, TaxReport taxReport) {
    return realiqPersistenceComponent.saveTaxReport(taxReport);
  }

  public TaxReport generateReport(ClientEstate estate) {
    var report = getTaxReport(estate);
    var transactions = estate.getTransactions();
    TaxReport taxReport;
    BigDecimal taxableIncome =
        calculateTotalByType(
            transactions, TransactionType.INCOME, Boolean.FALSE, Integer.valueOf(2025));
    BigDecimal deductibleExpenses =
        calculateTotalByType(
            transactions, TransactionType.EXPENSE, Boolean.TRUE, Integer.valueOf(2025));
    BigDecimal otherExpenses =
        calculateTotalByType(
            transactions, TransactionType.EXPENSE, Boolean.FALSE, Integer.valueOf(2025));
    BigDecimal totalExpenses = deductibleExpenses.add(otherExpenses);
    BigDecimal totalTaxableIncome = taxableIncome.multiply(BigDecimal.valueOf(0.7));
    var taxRate = TaxRate.FLAT;

    BigDecimal totalTax = totalTaxableIncome.multiply(BigDecimal.valueOf(0.15));

    if (report.isEmpty()) {
      var newReport =
          new TaxReport(
              estate,
              Integer.valueOf(2025),
              taxableIncome,
              deductibleExpenses,
              otherExpenses,
              totalExpenses,
              totalTaxableIncome,
              totalTax,
              taxRate);
      taxReport = saveReport(false, newReport);
    } else {
      var updatedReport = report.getFirst();
      updatedReport.setTaxableIncome(taxableIncome);
      updatedReport.setDeductibleExpenses(deductibleExpenses);
      updatedReport.setOtherExpenses(otherExpenses);
      updatedReport.setTotalExpenses(totalExpenses);
      updatedReport.setTotalTaxableIncome(totalTaxableIncome);
      updatedReport.setTotalTax(totalTax);
      updatedReport.setTaxRate(taxRate);
      taxReport = saveReport(true, updatedReport);
    }

    return taxReport;
  }

  public ClientEstate getClientEstateBy(Long id) {
    return realiqPersistenceComponent.getClientEstate(id);
  }

  public void deleteClientEstate(ClientEstate estate) {
    var estateWatchDogs = getWatchDogsByRealiqId(estate.getId().intValue());
    for (WatchDogEntity watchDog : estateWatchDogs) {
      deleteWatchDog(watchDog);
    }
    realiqPersistenceComponent.deleteClientEstate(estate);
  }

  @Transactional
  public String setValuesToEstateAndSave(
      Map<String, Object> requestJson, ClientEstate clientEstate) {
    if (clientEstate.getId() == null) {
      realiqPersistenceComponent.validateEstateCreationLimit(clientEstate.getClient());
    }
    clientEstate = realiqPersistenceComponent.createClientEstateAndSave(requestJson, clientEstate);
    Boolean isNew = (Boolean) requestJson.get("isNew");
    Boolean doResetWatchDog =
        requestJson.containsKey("doResetWatchDog")
            ? (Boolean) requestJson.get("doResetWatchDog")
            : Boolean.FALSE;
    if (isNew) {
      realiqPersistenceComponent.convertEstateToWatchDogAndSave(clientEstate, doResetWatchDog);
    } else if (doResetWatchDog) {
      realiqPersistenceComponent.convertEstateToWatchDogAndSave(clientEstate, doResetWatchDog);
    }

    return EDIT_SUCCESS_SIMPLE;
  }

  @Transactional
  public void doActionWithTransaction(
      List<Map<String, Object>> transactions, CoreClientEntity client) {
    Map<String, BankImportActivity> bankImports = new LinkedHashMap<>();
    for (Map<String, Object> transaction : transactions) {
      ActionEnum type = ActionEnum.valueOf(transaction.get("type").toString());
      Number estateId = (Number) transaction.get("estateId");
      if (estateId == null) {
        throw new SoftiqException("Missing estate ID");
      }
      ClientEstate estate = getClientEstateBy(estateId.longValue());
      if (estate == null || !client.equals(estate.getClient())) {
        throw new SoftiqException("Estate not found");
      }
      if (!type.equals(ActionEnum.ADD)) {
        Number transactionId = (Number) transaction.get("id");
        if (transactionId == null || transactionId.longValue() == 0) {
          throw new SoftiqException("Missing transaction ID");
        }
        Transaction existingTransaction =
            transactionComponent.getTransaction(transactionId.longValue());
        if (!existingTransaction.getEstate().getId().equals(estate.getId())) {
          throw new SoftiqException("Transaction not found");
        }
      }
      if (type.equals(ActionEnum.ADD) || type.equals(ActionEnum.EDIT)) {
        if (type.equals(ActionEnum.ADD)) {
          Object bankImportId = transaction.get("bankImportId");
          if (bankImportId != null
              && transactionComponent.bankImportExists(
                  estateId.longValue(), bankImportId.toString())) {
            continue;
          }
        }
        Transaction newTransaction = convertToTransaction(transaction);
        if (type.equals(ActionEnum.ADD)) {
          boolean replacedById = false;
          if (transaction.containsKey("replaceRecurringTransactionId")) {
            Number replaceRecurringTransactionId =
                (Number) transaction.get("replaceRecurringTransactionId");
            if (replaceRecurringTransactionId != null) {
              transactionComponent.endRecurringTransactionById(
                  newTransaction, replaceRecurringTransactionId.longValue());
              replacedById = true;
            }
          }
          if (!replacedById) {
            transactionComponent.endPreviousImportedRecurringTransaction(newTransaction);
          }
          Object bankImportId = transaction.get("bankImportId");
          if (bankImportId != null && !bankImportId.toString().isBlank()) {
            Object verifiedPeriods = transaction.get("verifiedPeriods");
            if (verifiedPeriods instanceof List<?> periods && !periods.isEmpty()) {
              for (Object period : periods) {
                if (period != null) {
                  transactionComponent.verifyPeriod(newTransaction, period.toString());
                }
              }
            } else {
              transactionComponent.verifyPeriodFromDate(newTransaction);
            }
          }
        }
        saveTransaction(newTransaction);
        if (type.equals(ActionEnum.ADD)) {
          collectBankImportActivity(bankImports, transaction, estate);
        }
      } else if (type.equals(ActionEnum.DELETE)) {
        Integer id = (Integer) transaction.get("id");
        Transaction existingTransaction = transactionComponent.getTransaction(id.longValue());
        deleteTransaction(existingTransaction);
      }
    }
    bankImports
        .values()
        .forEach(
            activity ->
                recordBankStatementImport(
                    client,
                    activity.estate(),
                    activity.fileName(),
                    activity.format(),
                    activity.transactionCount(),
                    List.copyOf(activity.periods())));
  }

  private void collectBankImportActivity(
      Map<String, BankImportActivity> bankImports,
      Map<String, Object> transaction,
      ClientEstate estate) {
    Object fileNameValue = transaction.get("bankImportFileName");
    Object formatValue = transaction.get("bankImportFormat");
    if (fileNameValue == null || formatValue == null) return;

    String fileName = fileNameValue.toString().trim();
    String format = formatValue.toString().trim().toUpperCase();
    if (fileName.isBlank() || fileName.length() > 255 || !Set.of("CSV", "GPC").contains(format)) {
      return;
    }

    Number transactionCountValue =
        transaction.get("bankImportTransactionCount") instanceof Number number ? number : null;
    int transactionCount =
        transactionCountValue == null ? 1 : Math.max(1, transactionCountValue.intValue());
    String key = estate.getId() + ":" + format + ":" + fileName;
    BankImportActivity activity =
        bankImports.computeIfAbsent(
            key,
            ignored ->
                new BankImportActivity(
                    estate, fileName, format, transactionCount, new LinkedHashSet<>()));

    Object importedPeriods = transaction.get("bankImportPeriods");
    if (!(importedPeriods instanceof List<?>)) {
      importedPeriods = transaction.get("verifiedPeriods");
    }
    if (importedPeriods instanceof List<?> periods) {
      periods.stream()
          .filter(period -> period != null && !period.toString().isBlank())
          .map(Object::toString)
          .forEach(activity.periods()::add);
    }
    if (activity.periods().isEmpty() && transaction.get("dateFrom") != null) {
      String dateFrom = transaction.get("dateFrom").toString();
      try {
        LocalDate date = LocalDate.parse(dateFrom.substring(0, 10));
        activity.periods().add(String.format("%02d %d", date.getMonthValue(), date.getYear()));
      } catch (RuntimeException ignored) {
        // The transaction validation reports malformed dates separately.
      }
    }
  }

  private record BankImportActivity(
      ClientEstate estate,
      String fileName,
      String format,
      int transactionCount,
      Set<String> periods) {}

  @Transactional
  public void verifyTransactionPeriods(
      Long estateId, List<Map<String, Object>> verifications, CoreClientEntity client) {
    ClientEstate estate = getClientEstateBy(estateId);
    if (estate == null || !client.equals(estate.getClient())) {
      throw new SoftiqException("Estate not found");
    }

    for (Map<String, Object> verification : verifications) {
      Number transactionId = (Number) verification.get("transactionId");
      Object period = verification.get("period");
      if (transactionId == null || period == null || period.toString().isBlank()) {
        throw new SoftiqException("Missing transaction verification data");
      }

      Transaction transaction = transactionComponent.getTransaction(transactionId.longValue());
      if (!transaction.getEstate().getId().equals(estateId)) {
        throw new SoftiqException("Transaction not found");
      }
      try {
        transactionComponent.verifyPeriod(transaction, period.toString());
      } catch (IllegalArgumentException exception) {
        throw new SoftiqException("Invalid transaction verification period");
      }
      saveTransaction(transaction);
    }
  }

  @Transactional
  public void sendTransactionReminder(
      Long estateId, Long transactionId, String period, CoreClientEntity client) {
    ClientEstate estate = getClientEstateBy(estateId);
    if (estate == null || !client.equals(estate.getClient())) {
      throw new SoftiqException("Estate not found");
    }
    if (estate.getTenantEmail() == null || estate.getTenantEmail().isBlank()) {
      throw new SoftiqException("Tenant email is missing");
    }

    Transaction transaction = transactionComponent.getTransaction(transactionId);
    if (!transaction.getEstate().getId().equals(estateId)) {
      throw new SoftiqException("Transaction not found");
    }
    if (!TransactionType.INCOME.equals(transaction.getTransactionType())) {
      throw new SoftiqException("Reminders can only be sent for income transactions");
    }
    if (transaction.getVerification() != null
        && transaction.getVerification().getListOfVerifiedTransactions() != null
        && transaction.getVerification().getListOfVerifiedTransactions().contains(period)) {
      throw new SoftiqException("Transaction is already verified");
    }

    try {
      ensureTransactionReminderAllowed(client, transaction.getId(), period);
      var membership = getWatchDogMembershipByCoreClient(client);
      String ownerName = null;
      if (membership != null) {
        ownerName = membership.getUserName();
      }
      var reminderEmail =
          transactionReminder(estate, transaction, period, client.getEmail(), ownerName);
      recordTransactionReminder(
          client, estate, transaction, period, reminderEmail.subject(), reminderEmail.htmlBody());
    } catch (IllegalArgumentException exception) {
      throw new SoftiqException("Invalid transaction verification period");
    }
  }
}
