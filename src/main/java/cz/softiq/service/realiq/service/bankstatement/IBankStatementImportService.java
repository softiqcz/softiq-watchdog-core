package cz.softiq.service.realiq.service.bankstatement;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.realiq.model.records.BankStatementImportResult;
import java.util.List;
import java.util.Map;

public interface IBankStatementImportService extends IBeanProvider, IBaseService {

  private BankStatementImportService getBankStatementImportService() {
    return getBean(BankStatementImportService.class);
  }

  default BankStatementImportResult parse(String fileName, String base64Content, Long estateId) {
    return getBankStatementImportService().parse(fileName, base64Content, estateId);
  }

  default List<Map<String, Object>> extractTransactions(Object transactions) {
    return getBankStatementImportService().extractTransactions(transactions);
  }

  default List<Map<String, Object>> extractItems(Object items, String fieldName) {
    return getBankStatementImportService().extractItems(items, fieldName);
  }
}
