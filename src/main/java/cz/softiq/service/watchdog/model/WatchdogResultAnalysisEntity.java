package cz.softiq.service.watchdog.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class WatchdogResultAnalysisEntity {
  private BigDecimal averagePrice;
  private BigDecimal averageSquareMeters;
  private BigDecimal averagePricePerSquareMeters;
}
