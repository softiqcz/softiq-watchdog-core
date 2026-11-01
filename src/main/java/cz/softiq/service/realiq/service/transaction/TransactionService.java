package cz.softiq.service.realiq.service.transaction;

import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionService {
  private final TransactionComponent transactionComponent;

  BigDecimal calculateTotalByType(
      List<Transaction> transactions,
      TransactionType transactionType,
      Boolean isTaxDeductible,
      Integer year) {
    return transactionComponent.calculateTotalByType(
        transactions, transactionType, isTaxDeductible, year);
  }

  Transaction convertToTransaction(Map<String, Object> transaction) {
    return transactionComponent.convertToTransaction(transaction);
  }

  void saveTransaction(Transaction transaction) {
    transactionComponent.saveTransaction(transaction);
  }

  void deleteTransaction(Transaction transaction) {
    transactionComponent.deleteTransaction(transaction);
  }
}
