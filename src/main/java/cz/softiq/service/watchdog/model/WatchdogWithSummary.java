package cz.softiq.service.watchdog.model;

import cz.softiq.service.watchdog.entity.WatchDogEntity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchdogWithSummary {
  private WatchDogEntity watchdog;
  private WatchdogMonthlySummary summary;
}
