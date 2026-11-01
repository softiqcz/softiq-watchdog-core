package cz.softiq.service.realiq.service.realiq;

import static cz.softiq.service.realiq.model.enums.EstateStatus.VACANT;
import static cz.softiq.service.watchdog.model.enums.SettingsEnums.EmailSettingEnum.GROUP_BY_ALL;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogPersistenceService;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.TaxReport;
import cz.softiq.service.realiq.model.enums.EstateStatus;
import cz.softiq.service.realiq.repository.ClientEstateRepository;
import cz.softiq.service.realiq.repository.TaxReportRepository;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import cz.softiq.service.watchdog.repository.WatchDogMembershipRepository;
import io.micrometer.common.util.StringUtils;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class RealiqPersistenceComponent implements IWatchDogPersistenceService {
  private final ClientEstateRepository clientEstateRepository;
  private final TaxReportRepository taxReportRepository;
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final WatchDogMembershipRepository watchDogMembershipRepository;
  private final WatchDogEstatesRepository watchDogEstatesRepository;

  public List<WatchDogEstatesEntity> getOffersForAnalysis(WatchDogEntity watchDog) {
    return watchDogEstatesRepository.findAllWatchDogEstateEntities(watchDog);
  }

  public ClientEstate getClientEstate(Long id) {
    return clientEstateRepository.findById(id).orElse(null);
  }

  public List<TaxReport> getTaxReports(ClientEstate estate) {
    return taxReportRepository.findTaxReportByEstate(estate);
  }

  public TaxReport saveTaxReport(TaxReport taxReport) {
    return taxReportRepository.save(taxReport);
  }

  public List<ClientEstate> getClientEstates(CoreClientEntity client) {
    return clientEstateRepository.findAllByClient(client);
  }

  public void validateEstateCreationLimit(CoreClientEntity client) {
    var membership =
        watchDogMembershipRepository
            .findFirstByIsActiveAndClient(true, client)
            .orElseThrow(() -> new SoftiqException("Aktivní členství nebylo nalezeno"));
    long estateCount = clientEstateRepository.countByClient(client);
    int estateLimit = membership.getMembership().getMaxCountOfEstates();

    if (estateCount >= estateLimit) {
      throw new SoftiqException("Nelze vytvořit novou nemovitost, byl dosažen limit členství");
    }
  }

  public ClientEstate setClientEstateFields(ClientEstate clientEstate) {
    return clientEstateRepository.save(clientEstate);
  }

  public void convertEstateToWatchDogAndSave(ClientEstate clientEstate, Boolean doResetWatchDog) {
    WatchDogEntity rentWatchdog = createBaseWatchdog(clientEstate, doResetWatchDog, 0);
    rentWatchdog.setOfferType("Pronájem");
    saveWatchDog(rentWatchdog);

    WatchDogEntity rentWatchdogReco = createBaseWatchdog(clientEstate, doResetWatchDog, 1);
    rentWatchdogReco.setEstateCondition("Novostavba; Po rekonstrukci; Velmi dobrý");
    rentWatchdogReco.setOfferType("Pronájem");
    saveWatchDog(rentWatchdogReco);

    WatchDogEntity saleWatchdog = createBaseWatchdog(clientEstate, doResetWatchDog, 2);
    saleWatchdog.setOfferType("Prodej");
    saveWatchDog(saleWatchdog);
  }

  private WatchDogEntity createBaseWatchdog(
      ClientEstate clientEstate, Boolean doResetWatchDog, int order) {
    WatchDogEntity watchDog;

    if (doResetWatchDog) {
      var estateWatchDogs = getWatchDogsByRealiqId(clientEstate.getId().intValue());
      watchDog = estateWatchDogs.get(order);
    } else {
      watchDog = new WatchDogEntity();
    }
    watchDog.setClient(clientEstate.getClient());
    watchDog.setRealiqId(clientEstate.getId().intValue());
    watchDog.setEstateType(clientEstate.getEstateType());
    watchDog.setFlatCategory(clientEstate.getFlatCategory());
    watchDog.setExactAddress(clientEstate.getExactAddress());
    watchDog.setSearchAddress(clientEstate.getExactAddress());
    watchDog.setEstateCondition(clientEstate.getEstateCondition());
    watchDog.setType(TypeEnum.CLIENT_ESTATE_WATCHDOG);
    watchDog.setUsableAreaTo(
        clientEstate.getUsableArea() != null
            ? clientEstate
                .getUsableArea()
                .add(new BigDecimal("10.0"))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString()
            : null);
    watchDog.setUsableAreaFrom(
        clientEstate.getUsableArea() != null
            ? clientEstate
                .getUsableArea()
                .subtract(new BigDecimal("10.0"))
                .setScale(0, RoundingMode.HALF_UP)
                .toPlainString()
            : null);
    watchDog.setRealityUrl("Sreality; Realitymat; Idnes; Bezrealitky");
    watchDog.setEmailReportFrequency(SettingsEnums.FrequencyEnum.WEEKLY);
    watchDog.setEmailReportSetting(GROUP_BY_ALL);
    watchDog.setInRange("Nezadáno");
    watchDog.setSendOfferAs(SettingsEnums.OfferSettingEnum.ALL);
    watchDog.setDoResetSearch(true);
    watchDog.setDoEmailReport(false);
    watchDog.setIsEnabled(true);
    watchDog.setIsFirstSyncCompleted(false);
    watchDog.setIsVisible(false);
    return watchDog;
  }

  public ClientEstate createClientEstateAndSave(
      Map<String, Object> requestJson, ClientEstate clientEstate) {
    Boolean isNew = Boolean.valueOf(clientEstate.getId() == null);
    EstateStatus status = VACANT;
    clientEstate.setName((String) requestJson.get("name"));
    clientEstate.setNote((String) requestJson.get("note"));

    String dateFromStr = (String) requestJson.get("dateFrom");
    String dateToStr = (String) requestJson.get("dateTo");

    if (dateFromStr != null && !dateFromStr.isBlank()) {
      Instant instantFrom = Instant.parse(dateFromStr);
      clientEstate.setDateFrom(Timestamp.from(instantFrom));
    }

    if (dateToStr != null && !dateToStr.isBlank()) {
      Instant instantTo = Instant.parse(dateToStr);
      clientEstate.setDateTo(Timestamp.from(instantTo));
      status = EstateStatus.SOLD;
    } else {
      clientEstate.setDateTo(null);
    }

    clientEstate.setEstateType((String) requestJson.get("estateType"));
    clientEstate.setFlatCategory((String) requestJson.get("flatCategory"));
    clientEstate.setExactAddress((String) requestJson.get("exactAddress"));
    clientEstate.setAddress((String) requestJson.get("address"));
    clientEstate.setOwnerShip((String) requestJson.get("ownerShip"));
    clientEstate.setEstateCondition((String) requestJson.get("estateCondition"));
    clientEstate.setAccessories(toStringList(requestJson.get("accessories")));
    clientEstate.setUsableArea(
        watchDogManagementComponent.toBigDecimal(requestJson.get("usableArea"), 2));
    clientEstate.setPurchasePrice(
        watchDogManagementComponent.toBigDecimal(requestJson.get("purchasePrice"), 0));
    String tenantName = (String) requestJson.get("tenantName");
    clientEstate.setTenantName(tenantName);
    if (!StringUtils.isBlank(tenantName)) {
      status = EstateStatus.RENTED;
    }
    clientEstate.setStatus(status);
    clientEstate.setTenantPhone((String) requestJson.get("tenantPhone"));
    clientEstate.setTenantEmail((String) requestJson.get("tenantEmail"));
    clientEstate.setTenantNote((String) requestJson.get("tenantNote"));
    clientEstate.setManagerName((String) requestJson.get("managerName"));
    clientEstate.setManagerPhone((String) requestJson.get("managerPhone"));
    clientEstate.setManagerEmail((String) requestJson.get("managerEmail"));
    clientEstate.setManagerNote((String) requestJson.get("managerNote"));
    return setClientEstateFields(clientEstate);
  }

  private List<String> toStringList(Object value) {
    if (value == null) {
      return List.of();
    }
    if (value instanceof String stringValue) {
      return Arrays.stream(stringValue.split("[;,]"))
          .map(String::trim)
          .filter(item -> !item.isBlank())
          .distinct()
          .toList();
    }
    if (value instanceof List<?> values) {
      return values.stream()
          .filter(Objects::nonNull)
          .map(Object::toString)
          .map(String::trim)
          .filter(item -> !item.isBlank())
          .distinct()
          .toList();
    }
    throw new SoftiqException("Unsupported accessories type: " + value.getClass());
  }

  public void deleteClientEstate(ClientEstate estate) {
    List<TaxReport> taxReports = taxReportRepository.findTaxReportByEstate(estate);
    taxReportRepository.deleteAll(taxReports);
    clientEstateRepository.delete(estate);
  }
}
