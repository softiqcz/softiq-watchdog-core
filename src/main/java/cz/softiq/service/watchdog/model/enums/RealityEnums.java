package cz.softiq.service.watchdog.model.enums;

import cz.softiq.common.base.model.IBaseModel;
import cz.softiq.core.configuration.SoftiqException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RealityEnums implements IBaseModel {
  SREALITY("Sreality"),
  BAZOS("Bazos"),
  IDNES("Idnes"),
  REALITYMAT("Realitymat"),
  CESKEREALITY("Ceskereality"),
  BEZREALITKY("Bezrealitky");

  private final String name;

  public static RealityEnums fromString(String name) {
    for (RealityEnums membership : RealityEnums.values()) {
      if (membership.name.equalsIgnoreCase(name)) {
        return membership;
      }
    }
    throw new SoftiqException("No enum constant with name: " + name);
  }
}
