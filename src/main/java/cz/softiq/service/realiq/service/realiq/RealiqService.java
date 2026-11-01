package cz.softiq.service.realiq.service.realiq;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.TaxReport;
import cz.softiq.service.realiq.model.records.EstateAnalysis;
import cz.softiq.service.realiq.model.records.EstateWatchDogOffer;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class RealiqService {
  private final RealiqComponent realiqComponent;

  List<ClientEstate> getAllUserEstate(CoreClientEntity client) {
    return realiqComponent.getAllUserEstate(client);
  }

  List<TaxReport> generateReports(CoreClientEntity client) {
    return realiqComponent.generateReports(client);
  }

  List<EstateWatchDogOffer> getUserWatchDogsResults(List<WatchDogEntity> watchDogEntities) {
    return realiqComponent.getUserWatchDogsResults(watchDogEntities);
  }

  EstateAnalysis getEstateAnalysis(List<WatchDogEntity> watchDogs) {
    return realiqComponent.getEstateAnalysis(watchDogs);
  }

  List<TaxReport> getTaxReport(ClientEstate estate) {
    return realiqComponent.getTaxReport(estate);
  }

  TaxReport saveReport(boolean isNew, TaxReport taxReport) {
    return realiqComponent.saveReport(isNew, taxReport);
  }

  TaxReport generateReport(ClientEstate estate) {
    return realiqComponent.generateReport(estate);
  }

  ClientEstate getClientEstateBy(Long id) {
    return realiqComponent.getClientEstateBy(id);
  }

  void deleteClientEstate(ClientEstate estate) {
    realiqComponent.deleteClientEstate(estate);
  }

  String setValuesToEstateAndSave(Map<String, Object> requestJson, ClientEstate clientEstate) {
    return realiqComponent.setValuesToEstateAndSave(requestJson, clientEstate);
  }

  void doActionWithTransaction(List<Map<String, Object>> transactions, CoreClientEntity client) {
    realiqComponent.doActionWithTransaction(transactions, client);
  }

  void verifyTransactionPeriods(
      Long estateId, List<Map<String, Object>> verifications, CoreClientEntity client) {
    realiqComponent.verifyTransactionPeriods(estateId, verifications, client);
  }

  void sendTransactionReminder(
      Long estateId, Long transactionId, String period, CoreClientEntity client) {
    realiqComponent.sendTransactionReminder(estateId, transactionId, period, client);
  }
}
