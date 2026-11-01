package cz.softiq.service.watchdog.model.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

public class SettingsEnums {

  @Getter
  @AllArgsConstructor
  public enum FrequencyEnum {
    OFTEN,
    DAILY,
    WEEKLY,
    NONSTOP
  }

  @Getter
  @AllArgsConstructor
  public enum EmailSettingEnum {
    GROUP_BY_ALL,
    GROUP_BY_WATCHDOG,
    SINGLE
  }

  @Getter
  @AllArgsConstructor
  public enum OfferSettingEnum {
    ALL,
    UPDATED,
    NEW
  }
}
