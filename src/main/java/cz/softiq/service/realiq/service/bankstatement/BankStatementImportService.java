package cz.softiq.service.realiq.service.bankstatement;

import cz.softiq.service.realiq.model.records.BankStatementImportResult;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class BankStatementImportService {
  private final BankStatementImportComponent bankStatementImportComponent;

  BankStatementImportResult parse(String fileName, String base64Content, Long estateId) {
    return bankStatementImportComponent.parse(fileName, base64Content, estateId);
  }

  List<Map<String, Object>> extractTransactions(Object transactions) {
    return bankStatementImportComponent.extractTransactions(transactions);
  }

  List<Map<String, Object>> extractItems(Object items, String fieldName) {
    return bankStatementImportComponent.extractItems(items, fieldName);
  }
}
