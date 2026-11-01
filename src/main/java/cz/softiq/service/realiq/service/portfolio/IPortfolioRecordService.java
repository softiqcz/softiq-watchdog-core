package cz.softiq.service.realiq.service.portfolio;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.enums.ActionEnum;
import java.util.List;
import java.util.Map;

public interface IPortfolioRecordService extends IBeanProvider, IBaseService {

  private PortfolioRecordService getPortfolioRecordService() {
    return getBean(PortfolioRecordService.class);
  }

  default List<Map<String, Object>> getRecords(CoreClientEntity client, ClientEstate estate) {
    return getPortfolioRecordService().getRecords(client, estate);
  }

  default Map<String, Object> getDocumentQuota(CoreClientEntity client) {
    return getPortfolioRecordService().getDocumentQuota(client);
  }

  default List<Map<String, Object>> doActionWithRecords(
      List<Map<String, Object>> records,
      CoreClientEntity client,
      ClientEstate estate,
      ActionEnum fallbackAction) {
    return getPortfolioRecordService().doActionWithRecords(records, client, estate, fallbackAction);
  }

  default List<Map<String, Object>> extractRecords(Map<String, Object> data) {
    return getPortfolioRecordService().extractRecords(data);
  }

  default void sendDueRecordReminders() {
    getPortfolioRecordService().sendDueRecordReminders();
  }
}
