package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.service.softiq.model.constants.MessageConstants.*;
import static java.lang.Boolean.TRUE;

import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.model.enums.*;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WatchDogSettingsComponent {
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final WatchDogPersistenceComponent watchDogPersistenceComponent;

  public String setValuesToWatchDogAndSave(
      Map<String, Object> requestJson, WatchDogEntity watchDog) {
    Boolean doResetSearch = (Boolean) requestJson.get("doResetSearch");
    Boolean isNew = watchDog.getId() == null;
    watchDog.setIsEnabled((Boolean) requestJson.get("isEnabled"));
    watchDog.setEstateType((String) requestJson.get("estateType"));
    watchDog.setOfferType((String) requestJson.get("offerType"));
    watchDog.setRealityUrl("Sreality; Realitymat; Idnes; Bezrealitky");
    watchDog.setExactAddress((String) requestJson.get("exactAddress"));
    watchDog.setInRange((String) requestJson.get("inRange"));
    watchDog.setFlatCategory(
        watchDogManagementComponent.joinStringList(requestJson.get("flatCategory")));
    watchDog.setHomeCategory(
        watchDogManagementComponent.joinStringList(requestJson.get("homeCategory")));
    watchDog.setLandCategory(
        watchDogManagementComponent.joinStringList(requestJson.get("landCategory")));
    watchDog.setComercialCategory(
        watchDogManagementComponent.joinStringList(requestJson.get("comercialCategory")));
    watchDog.setOtherCategory(
        watchDogManagementComponent.joinStringList(requestJson.get("otherCategory")));
    watchDog.setEstateCondition(
        watchDogManagementComponent.joinStringList(requestJson.get("estateCondition")));
    watchDog.setFloorFrom(String.valueOf(requestJson.get("floorFrom")));
    watchDog.setFloorTo(String.valueOf(requestJson.get("floorTo")));
    watchDog.setUsableAreaFrom(String.valueOf(requestJson.get("usableAreaFrom")));
    watchDog.setUsableAreaTo(String.valueOf(requestJson.get("usableAreaTo")));
    watchDog.setPriceFrom(String.valueOf(requestJson.get("priceFrom")));
    watchDog.setPriceTo(String.valueOf(requestJson.get("priceTo")));
    watchDog.setDoEmailReport((Boolean) requestJson.get("doEmailReport"));
    watchDog.setIsVisible(TRUE);
    String frequencyStr = (String) requestJson.get("emailReportFrequency");
    watchDog.setEmailReportFrequency(SettingsEnums.FrequencyEnum.valueOf(frequencyStr));
    if (watchDog.getDoEmailReport()) {
      String emailReportSetting = (String) requestJson.get("emailReportSetting");
      watchDog.setEmailReportSetting(SettingsEnums.EmailSettingEnum.valueOf(emailReportSetting));
    } else {
      watchDog.setEmailReportSetting(SettingsEnums.EmailSettingEnum.GROUP_BY_ALL);
    }
    String sendOfferAs = (String) requestJson.get("sendOfferAs");
    watchDog.setType(TypeEnum.AUTOMATION_WATCHDOG);
    watchDog.setSendOfferAs(SettingsEnums.OfferSettingEnum.valueOf(sendOfferAs));
    watchDog.setIsFirstSyncCompleted(!isNew && !doResetSearch);
    watchDog.setClient(watchDogManagementComponent.getClient());
    watchDogPersistenceComponent.saveNewWatchDog(watchDog);
    watchDogManagementComponent.createOrEditNewChildren(watchDog, isNew);
    return isNew ? WATCHDOG_CREATED : WATCHDOG_EDIT_SUCCESS;
  }
}
