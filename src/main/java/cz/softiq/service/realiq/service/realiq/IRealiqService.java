package cz.softiq.service.realiq.service.realiq;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.TaxReport;
import cz.softiq.service.realiq.model.records.EstateAnalysis;
import cz.softiq.service.realiq.model.records.EstateWatchDogOffer;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.List;
import java.util.Map;

public interface IRealiqService extends IBeanProvider, IBaseService {

  // REALIQ
  private RealiqService getRealiqService() {
    return getBean(RealiqService.class);
  }

  default List<ClientEstate> getAllUserEstate(CoreClientEntity client) {
    return getRealiqService().getAllUserEstate(client);
  }

  default List<TaxReport> generateReports(CoreClientEntity client) {
    return getRealiqService().generateReports(client);
  }

  default List<EstateWatchDogOffer> getUserWatchDogsResults(List<WatchDogEntity> watchDogEntities) {
    return getRealiqService().getUserWatchDogsResults(watchDogEntities);
  }

  default EstateAnalysis getEstateAnalysis(List<WatchDogEntity> watchDogs) {
    return getRealiqService().getEstateAnalysis(watchDogs);
  }

  default List<TaxReport> getTaxReport(ClientEstate clientEstate) {
    return getRealiqService().getTaxReport(clientEstate);
  }

  default TaxReport saveReport(boolean isNew, TaxReport taxReport) {
    return getRealiqService().saveReport(isNew, taxReport);
  }

  default ClientEstate getClientEstateBy(Long id) {
    return getRealiqService().getClientEstateBy(id);
  }

  default void deleteClientEstate(ClientEstate estate) {
    getRealiqService().deleteClientEstate(estate);
  }

  default void doActionWithTransaction(
      List<Map<String, Object>> transactions, CoreClientEntity client) {
    getRealiqService().doActionWithTransaction(transactions, client);
  }

  default void verifyTransactionPeriods(
      Long estateId, List<Map<String, Object>> verifications, CoreClientEntity client) {
    getRealiqService().verifyTransactionPeriods(estateId, verifications, client);
  }

  default void sendTransactionReminder(
      Long estateId, Long transactionId, String period, CoreClientEntity client) {
    getRealiqService().sendTransactionReminder(estateId, transactionId, period, client);
  }

  default String setValuesToEstateAndSave(
      Map<String, Object> requestJson, ClientEstate clientEstate) {
    return getRealiqService().setValuesToEstateAndSave(requestJson, clientEstate);
  }

  default TaxReport generateReport(ClientEstate estate) {
    return getRealiqService().generateReport(estate);
  }
}
