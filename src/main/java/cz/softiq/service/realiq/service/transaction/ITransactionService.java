package cz.softiq.service.realiq.service.transaction;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.TransactionType;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface ITransactionService extends IBeanProvider, IBaseService {

  private TransactionService getTransactionService() {
    return getBean(TransactionService.class);
  }

  default BigDecimal calculateTotalByType(
      List<Transaction> transactions,
      TransactionType transactionType,
      Boolean isTaxDeductible,
      Integer year) {
    return getTransactionService()
        .calculateTotalByType(transactions, transactionType, isTaxDeductible, year);
  }

  default Transaction convertToTransaction(Map<String, Object> transaction) {
    return getTransactionService().convertToTransaction(transaction);
  }

  default void saveTransaction(Transaction transaction) {
    getTransactionService().saveTransaction(transaction);
  }

  default void deleteTransaction(Transaction transaction) {
    getTransactionService().deleteTransaction(transaction);
  }
}
