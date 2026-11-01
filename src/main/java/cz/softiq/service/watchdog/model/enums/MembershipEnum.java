package cz.softiq.service.watchdog.model.enums;

import cz.softiq.common.base.model.IBaseModel;
import cz.softiq.core.configuration.SoftiqException;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MembershipEnum implements IBaseModel {
  PENDING("PENDING", new BigDecimal("0.0"), 0, 0, "pending", 0),
  REGISTER("REGISTER", new BigDecimal("0.0"), 0, 0, "Započal registraci", 0),
  BASIC("BASIC", new BigDecimal("0.0"), 1, 1, "1 hlídací pes", 7),
  STANDARD("STANDARD", new BigDecimal("49.0"), 2, 2, "až 2 hlídací psi", 7),
  PREMIUM("PREMIUM", new BigDecimal("129.0"), 3, 5, "až 3 hlídací psi", 30),
  PROFESSIONAL("PROFESSIONAL", new BigDecimal("249.0"), 5, 15, "až 5 hlídacích psů", 30000);

  private final String name;
  private final BigDecimal price;
  private final int maxCountOfWatchdogs;
  private final int maxCountOfEstates;
  private final String maxCountOfWatchdogsText;
  private final int maxDaysToKeepHistory;

  public static MembershipEnum fromString(String name) {
    for (MembershipEnum membership : MembershipEnum.values()) {
      if (membership.name.equalsIgnoreCase(name)) {
        return membership;
      }
    }
    throw new SoftiqException("No enum constant with name: " + name);
  }
}
