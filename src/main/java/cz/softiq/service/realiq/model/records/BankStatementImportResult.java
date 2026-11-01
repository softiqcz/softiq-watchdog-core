package cz.softiq.service.realiq.model.records;

import java.math.BigDecimal;
import java.util.List;

public record BankStatementImportResult(
    String fileName,
    String format,
    int transactionCount,
    List<BankStatementTransaction> transactions) {

  public record BankStatementTransaction(
      String importId,
      String seriesId,
      String externalId,
      String date,
      BigDecimal amount,
      String currency,
      String transactionType,
      String suggestedName,
      String counterpartyName,
      String counterpartyAccount,
      String variableSymbol,
      String message,
      String bankTransactionType,
      boolean alreadyImported,
      String category,
      ExistingRecurringTransaction existingRecurringTransaction,
      List<ExistingRecurringTransaction> recurringTransactionCandidates) {

    public BankStatementTransaction withAlreadyImported(boolean value) {
      return new BankStatementTransaction(
          importId,
          seriesId,
          externalId,
          date,
          amount,
          currency,
          transactionType,
          suggestedName,
          counterpartyName,
          counterpartyAccount,
          variableSymbol,
          message,
          bankTransactionType,
          value,
          category,
          existingRecurringTransaction,
          recurringTransactionCandidates);
    }

    public BankStatementTransaction withMatch(
        String resolvedCategory,
        ExistingRecurringTransaction existingTransaction,
        List<ExistingRecurringTransaction> candidates) {
      return new BankStatementTransaction(
          importId,
          seriesId,
          externalId,
          date,
          amount,
          currency,
          transactionType,
          suggestedName,
          counterpartyName,
          counterpartyAccount,
          variableSymbol,
          message,
          bankTransactionType,
          alreadyImported,
          resolvedCategory,
          existingTransaction,
          candidates);
    }
  }

  public record ExistingRecurringTransaction(
      Long id, String name, BigDecimal amount, String recurrence, String dateFrom, String dateTo) {}
}
