package cz.softiq.service.watchdog.model.enums;

import cz.softiq.common.base.model.IBaseModel;
import java.math.BigDecimal;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PromoCodeEnum implements IBaseModel {
  SOFT1CZK4EVER("SOFT1CZK4EVER", new BigDecimal("0.01"), 1, MembershipEnum.PROFESSIONAL, 5, false),
  OKWHG("OKWHG", new BigDecimal("0.5"), 50, null, 5, false),
  PIYETOAHGTWJ("PIYETOAHGTWJ", new BigDecimal("0.5"), 50, null, 5, false),
  SOFTFREE2026("SOFTFREE2026", BigDecimal.ZERO, 12, MembershipEnum.PREMIUM, 1, true);

  private final String name;
  private final BigDecimal multiplier;
  private final Integer maxRedemptions;
  private final MembershipEnum membership;
  private final Integer maxWatchdogs;
  private final boolean lifetime;

  public boolean appliesTo(MembershipEnum membership) {
    return this.membership == null || this.membership == membership;
  }

  public static PromoCodeEnum fromString(String name) {
    return Arrays.stream(values())
        .filter(p -> p.name.equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }
}
