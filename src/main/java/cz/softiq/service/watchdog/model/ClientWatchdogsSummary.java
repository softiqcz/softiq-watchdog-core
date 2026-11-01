package cz.softiq.service.watchdog.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClientWatchdogsSummary {
  private List<WatchdogWithSummary> watchdogSummaries = new ArrayList<>();

  public boolean hasWatchdogs() {
    return watchdogSummaries != null && !watchdogSummaries.isEmpty();
  }

  public int getTotalWatchdogCount() {
    return watchdogSummaries == null ? 0 : watchdogSummaries.size();
  }

  public int getTotalOfferCount() {
    if (watchdogSummaries == null) {
      return 0;
    }
    return watchdogSummaries.stream()
        .mapToInt(ws -> ws.getSummary().getCurrentMonthOfferCount())
        .sum();
  }
}
