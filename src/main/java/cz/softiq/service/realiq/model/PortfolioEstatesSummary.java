package cz.softiq.service.realiq.model;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioEstatesSummary {
  private List<PortfolioEstateMonthlySummary> estatesSummaries = new ArrayList<>();
  private PortfolioMonthlySummary totalSummary;

  public boolean hasEstates() {
    return estatesSummaries != null && !estatesSummaries.isEmpty();
  }

  public boolean hasMultipleEstates() {
    return estatesSummaries != null && estatesSummaries.size() > 1;
  }
}
