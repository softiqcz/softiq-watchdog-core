package cz.softiq.service.realiq.service.portfolio;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.enums.ActionEnum;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class PortfolioRecordService {
  private final PortfolioRecordComponent portfolioRecordComponent;

  List<Map<String, Object>> getRecords(CoreClientEntity client, ClientEstate estate) {
    return portfolioRecordComponent.getRecords(client, estate);
  }

  Map<String, Object> getDocumentQuota(CoreClientEntity client) {
    return portfolioRecordComponent.getDocumentQuota(client);
  }

  List<Map<String, Object>> doActionWithRecords(
      List<Map<String, Object>> records,
      CoreClientEntity client,
      ClientEstate estate,
      ActionEnum fallbackAction) {
    return portfolioRecordComponent.doActionWithRecords(records, client, estate, fallbackAction);
  }

  List<Map<String, Object>> extractRecords(Map<String, Object> data) {
    return portfolioRecordComponent.extractRecords(data);
  }

  void sendDueRecordReminders() {
    portfolioRecordComponent.sendDueRecordReminders();
  }
}
