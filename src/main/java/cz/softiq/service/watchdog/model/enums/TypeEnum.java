package cz.softiq.service.watchdog.model.enums;

import cz.softiq.common.base.model.IBaseModel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TypeEnum implements IBaseModel {
  CLIENT_ESTATE_WATCHDOG,
  AUTOMATION_WATCHDOG;
}
