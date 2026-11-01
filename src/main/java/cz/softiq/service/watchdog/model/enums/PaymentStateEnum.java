package cz.softiq.service.watchdog.model.enums;

import cz.softiq.common.base.model.IBaseModel;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PaymentStateEnum implements IBaseModel {
  CREATED("CREATED", "Platba nedokončena"),
  PAID("PAID", null),
  CANCELED("CANCELED", "Platba zrušena"),
  TIMEOUTED("TIMEOUTED", "Vypršela platnost"),
  FINISHED("FINISHED", "Opakovaná platba zrušena"),
  PAYMENT_RECURRENCE_STOPPED("PAYMENT_RECURRENCE_STOPPED", "Opakovaná platba zrušena"),
  PAYMENT_METHOD_CHOSEN("PAYMENT_METHOD_CHOSEN", "Platba zrušena"),
  NO_PAYMENT("NO_PAYMENT", "Účet deaktivován"),
  STOPPED("STOPPED", "Platba zrušena zákazníkem"),
  RECURRENCE_OK("RECURRENCE_OK", "Opakovaná platba OK."),
  ERROR_1("ERROR_1", "Platba již byla zrušena"),
  ERROR_2("ERROR_2", "Neznámý kód chyby, kontaktujte podporu info@softiq.cz s číslem 4867"),
  ERROR_3("ERROR_3", "Neznámý kód chyby, kontaktujte podporu info@softiq.cz s číslem 8755"),
  ERROR_4("ERROR_4", "Neznámý kód chyby, kontaktujte podporu info@softiq.cz s číslem 9775"),
  ERROR_5("ERROR_5", "Neznámý kód chyby, kontaktujte podporu info@softiq.cz s číslem 1823"),
  ERROR_6("ERROR_6", "Neznámý kód chyby, kontaktujte podporu info@softiq.cz s číslem 5789");

  private final String state;
  private final String message;

  public static PaymentStateEnum fromState(String state) {
    return Arrays.stream(values())
        .filter(e -> e.state.equalsIgnoreCase(state))
        .findFirst()
        .orElse(null);
  }
}
