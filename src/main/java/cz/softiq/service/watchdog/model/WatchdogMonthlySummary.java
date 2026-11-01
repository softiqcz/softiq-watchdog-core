package cz.softiq.service.watchdog.model;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchdogMonthlySummary {
  private int currentMonthOfferCount;
  private BigDecimal currentMonthAveragePrice;
  private BigDecimal currentMonthAveragePricePerSquareMeter;
  private BigDecimal currentMonthAverageSquareMeters;

  private int previousMonthOfferCount;
  private BigDecimal previousMonthAveragePrice;
  private BigDecimal previousMonthAveragePricePerSquareMeter;
  private BigDecimal previousMonthAverageSquareMeters;

  // Trend indicators
  private String priceTrend; // "up", "down", "stable"
  private String pricePerSquareMeterTrend;
  private String sizeTrend;
  private String offerCountTrend;

  // Percentage changes
  private BigDecimal priceChangePercent;
  private BigDecimal pricePerSquareMeterChangePercent;
  private BigDecimal sizeChangePercent;
  private int offerCountChange;

  public String getPriceTrendText() {
    return getTrendText(priceTrend, "Průměrná cena", priceChangePercent);
  }

  public String getPricePerSquareMeterTrendText() {
    return getTrendText(pricePerSquareMeterTrend, "Cena za m²", pricePerSquareMeterChangePercent);
  }

  public String getSizeTrendText() {
    return getTrendText(sizeTrend, "Průměrná plocha", sizeChangePercent);
  }

  public String getOfferCountTrendText() {
    if ("up".equals(offerCountTrend)) {
      return "Objevilo se o " + Math.abs(offerCountChange) + " nabídek více než v minulém měsíci";
    } else if ("down".equals(offerCountTrend)) {
      return "Objevilo se o " + Math.abs(offerCountChange) + " nabídek méně než v minulém měsíci";
    } else {
      return "Počet nabídek zůstal stejný jako minulý měsíc";
    }
  }

  public String getPricePerSquareMeterExplanation() {
    if (pricePerSquareMeterTrend == null || "stable".equals(pricePerSquareMeterTrend)) {
      return "cena za m² zůstala stabilní";
    }
    double percent =
        pricePerSquareMeterChangePercent != null
            ? pricePerSquareMeterChangePercent.abs().doubleValue()
            : 0;
    if ("up".equals(pricePerSquareMeterTrend)) {
      if (percent > 10) {
        return "cena za m² výrazně vzrostla";
      } else if (percent > 5) {
        return "cena za m² vzrostla o něco více";
      } else {
        return "cena za m² lehce vzrostla";
      }
    } else {
      if (percent > 10) {
        return "cena za m² výrazně klesla";
      } else if (percent > 5) {
        return "cena za m² klesla o něco více";
      } else {
        return "cena za m² lehce klesla";
      }
    }
  }

  public String getPriceExplanation() {
    if (priceTrend == null || "stable".equals(priceTrend)) {
      return "nabídky mají podobnou cenu";
    }
    double percent = priceChangePercent != null ? priceChangePercent.abs().doubleValue() : 0;
    if ("up".equals(priceTrend)) {
      if (percent > 10) {
        return "nabídky jsou výrazně dražší";
      } else if (percent > 5) {
        return "nabídky jsou o něco dražší";
      } else {
        return "nabídky jsou lehce dražší";
      }
    } else {
      if (percent > 10) {
        return "nabídky jsou výrazně levnější";
      } else if (percent > 5) {
        return "nabídky jsou o něco levnější";
      } else {
        return "nabídky jsou lehce levnější";
      }
    }
  }

  public String getSizeExplanation() {
    if (sizeTrend == null || "stable".equals(sizeTrend)) {
      return "nabídky mají podobnou plochu";
    }
    double percent = sizeChangePercent != null ? sizeChangePercent.abs().doubleValue() : 0;
    if ("up".equals(sizeTrend)) {
      if (percent > 10) {
        return "nabídky nabízejí výrazně více prostoru";
      } else if (percent > 5) {
        return "nabídky jsou o něco větší";
      } else {
        return "nabídky jsou lehce větší";
      }
    } else {
      if (percent > 10) {
        return "nabídky nabízejí výrazně méně prostoru";
      } else if (percent > 5) {
        return "nabídky jsou o něco menší";
      } else {
        return "nabídky jsou lehce menší";
      }
    }
  }

  private String getTrendText(String trend, String metric, BigDecimal changePercent) {
    if ("up".equals(trend)) {
      return metric + " vzrostla o " + formatPercent(changePercent) + " oproti minulému měsíci";
    } else if ("down".equals(trend)) {
      return metric + " klesla o " + formatPercent(changePercent.abs()) + " oproti minulému měsíci";
    } else {
      return metric + " se oproti minulému měsíci nezměnila";
    }
  }

  private String formatPercent(BigDecimal percent) {
    if (percent == null) {
      return "0%";
    }
    return percent.abs().setScale(1, java.math.RoundingMode.HALF_UP) + "%";
  }
}
