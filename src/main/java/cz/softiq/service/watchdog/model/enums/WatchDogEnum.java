package cz.softiq.service.watchdog.model.enums;

import cz.softiq.common.base.model.IBaseModel;
import cz.softiq.core.configuration.SoftiqException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum WatchDogEnum implements IBaseModel {
  DOMY("Domy", "house"),
  BYTY("Byty", "flat"),
  POZEMKY("Pozemky", "land"),
  OSTATNI("Ostatní", "commercial"),
  PRODEJ("Prodej", "sale"),
  PRONAJEM("Pronájem", "lease"),
  COMMERCIAL("Komerční", "commercial");

  private final String name;
  private final String nameIdnes;

  public static WatchDogEnum fromString(String name) {
    for (WatchDogEnum membership : WatchDogEnum.values()) {
      if (membership.name.equalsIgnoreCase(name)) {
        return membership;
      }
    }
    throw new SoftiqException("No enum constant with name: " + name);
  }
}
