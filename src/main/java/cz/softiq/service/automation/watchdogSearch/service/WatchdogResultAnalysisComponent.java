package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.model.WatchdogMonthlySummary;
import cz.softiq.service.watchdog.model.WatchdogResultAnalysisEntity;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WatchdogResultAnalysisComponent {
  private static final int MINIMUM_RECENT_RESULTS = 5;
  private static final int FALLBACK_RESULT_LIMIT = 30;
  private static final BigDecimal SOFTIQ_INDEX_SCALE = BigDecimal.valueOf(1.5);

  private final WatchDogEstatesRepository watchDogEstatesRepository;

  public WatchdogResultAnalysisEntity analyze(
      WatchDogEntity watchDog, List<WatchDogEstatesEntity> currentOffers) {
    List<WatchDogEstatesEntity> offers = getAnalysisOffers(watchDog, currentOffers);
    offers.forEach(WatchDogOfferUtils::populateMissingAreaAndPricePerSquareMeter);
    return new WatchdogResultAnalysisEntity(
        average(offers, WatchDogEstatesEntity::getPrice, 0),
        average(offers, WatchDogEstatesEntity::getSquareMeter, 1),
        average(offers, WatchDogEstatesEntity::getPricePerSquareMeter, 0));
  }

  public void populateSoftiqIndexes(
      WatchDogEntity watchDog, List<WatchDogEstatesEntity> currentOffers) {
    WatchdogResultAnalysisEntity analysis = analyze(watchDog, currentOffers);
    currentOffers.forEach(
        offer -> {
          offer.setSoftiqIndex(calculateSoftiqIndex(offer, analysis));
          offer.setComparisonText(generateComparisonText(offer, analysis));
        });
  }

  private BigDecimal calculateSoftiqIndex(
      WatchDogEstatesEntity offer, WatchdogResultAnalysisEntity analysis) {
    Optional<BigDecimal> pricePerSquareMeter = parsePositiveDecimal(offer.getPricePerSquareMeter());
    Optional<BigDecimal> squareMeters = parsePositiveDecimal(offer.getSquareMeter());
    if (pricePerSquareMeter.isEmpty() || squareMeters.isEmpty() || !hasPositiveAverages(analysis)) {
      return null;
    }

    BigDecimal pricePerSquareMeterDiff =
        calculateDifference(pricePerSquareMeter.get(), analysis.getAveragePricePerSquareMeters());
    BigDecimal squareMetersDiff =
        calculateDifference(squareMeters.get(), analysis.getAverageSquareMeters());

    BigDecimal softiqIndex =
        squareMetersDiff
            .subtract(pricePerSquareMeterDiff)
            .multiply(SOFTIQ_INDEX_SCALE)
            .setScale(2, RoundingMode.HALF_UP);

    return softiqIndex.max(BigDecimal.TEN.negate()).min(BigDecimal.TEN);
  }

  private String generateComparisonText(
      WatchDogEstatesEntity offer, WatchdogResultAnalysisEntity analysis) {
    Optional<BigDecimal> pricePerSquareMeter = parsePositiveDecimal(offer.getPricePerSquareMeter());
    Optional<BigDecimal> squareMeters = parsePositiveDecimal(offer.getSquareMeter());
    if (pricePerSquareMeter.isEmpty() || squareMeters.isEmpty() || !hasPositiveAverages(analysis)) {
      return null;
    }

    BigDecimal pricePerM2Diff =
        calculateDifference(pricePerSquareMeter.get(), analysis.getAveragePricePerSquareMeters());
    BigDecimal squareMetersDiff =
        calculateDifference(squareMeters.get(), analysis.getAverageSquareMeters());

    String priceText = getPriceComparisonText(pricePerM2Diff);
    String sizeText = getSizeComparisonText(squareMetersDiff);

    return priceText + sizeText;
  }

  private String getPriceComparisonText(BigDecimal pricePerM2Diff) {
    double diff = pricePerM2Diff.doubleValue();
    if (diff == 0) {
      return "Cena za m² odpovídá průměru v oblasti";
    } else if (diff > 1.5) {
      return "Cena za m² je výrazně vyšší než v okolí";
    } else if (diff > 0.5) {
      return "Cena za m² je o něco vyšší než v okolí";
    } else if (diff > 0) {
      return "Cena za m² je lehce nadprůměrná";
    } else if (diff > -0.5) {
      return "Cena za m² je lehce podprůměrná";
    } else if (diff > -1.5) {
      return "Cena za m² je o něco nižší než v okolí";
    } else {
      return "Cena za m² je výrazně nižší než v okolí";
    }
  }

  private String getSizeComparisonText(BigDecimal squareMetersDiff) {
    double diff = squareMetersDiff.doubleValue();
    if (diff == 0) {
      return ", plochou odpovídá průměru.";
    } else if (diff > 1.5) {
      return " a nabízí výrazně více prostoru než ostatní nabídky ve vašem filtru.";
    } else if (diff > 0.5) {
      return " a plochou je o něco větší než ostatní nabídky ve vašem filtru.";
    } else if (diff > 0) {
      return " a plochou je lehce větší než ostatní nabídky ve vašem filtru.";
    } else if (diff > -0.5) {
      return " a plochou je lehce menší než ostatní nabídky ve vašem filtru.";
    } else if (diff > -1.5) {
      return " a plochou je o něco menší než ostatní nabídky ve vašem filtru.";
    } else {
      return " a nabízí výrazně méně prostoru než ostatní nabídky ve vašem filtru.";
    }
  }

  private boolean hasPositiveAverages(WatchdogResultAnalysisEntity analysis) {
    return analysis.getAveragePricePerSquareMeters() != null
        && analysis.getAveragePricePerSquareMeters().signum() > 0
        && analysis.getAverageSquareMeters() != null
        && analysis.getAverageSquareMeters().signum() > 0;
  }

  private BigDecimal calculateDifference(BigDecimal value, BigDecimal average) {
    return value
        .subtract(average)
        .divide(average, 4, RoundingMode.HALF_UP)
        .multiply(BigDecimal.TEN);
  }

  private List<WatchDogEstatesEntity> getAnalysisOffers(
      WatchDogEntity watchDog, List<WatchDogEstatesEntity> currentOffers) {
    LocalDate today = LocalDate.now();
    List<WatchDogEstatesEntity> recentOffers =
        watchDogEstatesRepository.findWatchDogEstatesEntitiesToShowBetween(
            watchDog, today.minusDays(30), today.plusDays(1));
    List<WatchDogEstatesEntity> combinedOffers = combine(currentOffers, recentOffers, null);
    if (combinedOffers.size() >= MINIMUM_RECENT_RESULTS) {
      return combinedOffers;
    }

    List<WatchDogEstatesEntity> latestOffers =
        watchDogEstatesRepository.findLatestWatchDogEstatesEntitiesByWatchdogEntity(
            watchDog, PageRequest.of(0, FALLBACK_RESULT_LIMIT));
    return combine(currentOffers, latestOffers, FALLBACK_RESULT_LIMIT);
  }

  private List<WatchDogEstatesEntity> combine(
      List<WatchDogEstatesEntity> currentOffers,
      List<WatchDogEstatesEntity> historicalOffers,
      Integer limit) {
    Map<String, WatchDogEstatesEntity> uniqueOffers = new LinkedHashMap<>();
    addOffers(uniqueOffers, currentOffers);
    addOffers(uniqueOffers, historicalOffers);
    return uniqueOffers.values().stream().limit(limit == null ? Long.MAX_VALUE : limit).toList();
  }

  private void addOffers(
      Map<String, WatchDogEstatesEntity> uniqueOffers, List<WatchDogEstatesEntity> offers) {
    if (offers == null) {
      return;
    }
    for (WatchDogEstatesEntity offer : offers) {
      if (offer != null) {
        uniqueOffers.putIfAbsent(offerKey(offer), offer);
      }
    }
  }

  private String offerKey(WatchDogEstatesEntity offer) {
    if (offer.getId() != null) {
      return "id:" + offer.getId();
    }
    return "provider:" + offer.getOrigin() + ":" + offer.getOfferId();
  }

  private BigDecimal average(
      List<WatchDogEstatesEntity> offers,
      Function<WatchDogEstatesEntity, String> valueExtractor,
      int scale) {
    List<BigDecimal> values = new ArrayList<>();
    for (WatchDogEstatesEntity offer : offers) {
      parsePositiveDecimal(valueExtractor.apply(offer)).ifPresent(values::add);
    }
    if (values.isEmpty()) {
      return null;
    }

    BigDecimal total = values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    return total.divide(BigDecimal.valueOf(values.size()), scale, RoundingMode.HALF_UP);
  }

  private Optional<BigDecimal> parsePositiveDecimal(String value) {
    if (value == null || value.isBlank()) {
      return Optional.empty();
    }

    String normalized = value.trim().replace(',', '.').replaceAll("[^0-9.]", "");
    try {
      BigDecimal parsedValue = new BigDecimal(normalized);
      return parsedValue.signum() > 0 ? Optional.of(parsedValue) : Optional.empty();
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  public WatchdogMonthlySummary analyzeMonthlySummary(
      WatchDogEntity watchDog, LocalDate currentMonth) {
    LocalDate currentMonthStart = currentMonth.withDayOfMonth(1);
    LocalDate currentMonthEnd = currentMonthStart.plusMonths(1);
    LocalDate previousMonthStart = currentMonthStart.minusMonths(1);
    LocalDate previousMonthEnd = currentMonthStart;

    List<WatchDogEstatesEntity> currentMonthOffers =
        watchDogEstatesRepository.findWatchDogEstatesEntitiesToShowBetween(
            watchDog, currentMonthStart, currentMonthEnd);
    List<WatchDogEstatesEntity> previousMonthOffers =
        watchDogEstatesRepository.findWatchDogEstatesEntitiesToShowBetween(
            watchDog, previousMonthStart, previousMonthEnd);

    currentMonthOffers.forEach(WatchDogOfferUtils::populateMissingAreaAndPricePerSquareMeter);
    previousMonthOffers.forEach(WatchDogOfferUtils::populateMissingAreaAndPricePerSquareMeter);

    WatchdogMonthlySummary summary = new WatchdogMonthlySummary();

    // Current month stats
    summary.setCurrentMonthOfferCount(currentMonthOffers.size());
    summary.setCurrentMonthAveragePrice(
        average(currentMonthOffers, WatchDogEstatesEntity::getPrice, 0));
    summary.setCurrentMonthAveragePricePerSquareMeter(
        average(currentMonthOffers, WatchDogEstatesEntity::getPricePerSquareMeter, 0));
    summary.setCurrentMonthAverageSquareMeters(
        average(currentMonthOffers, WatchDogEstatesEntity::getSquareMeter, 1));

    // Previous month stats
    summary.setPreviousMonthOfferCount(previousMonthOffers.size());
    summary.setPreviousMonthAveragePrice(
        average(previousMonthOffers, WatchDogEstatesEntity::getPrice, 0));
    summary.setPreviousMonthAveragePricePerSquareMeter(
        average(previousMonthOffers, WatchDogEstatesEntity::getPricePerSquareMeter, 0));
    summary.setPreviousMonthAverageSquareMeters(
        average(previousMonthOffers, WatchDogEstatesEntity::getSquareMeter, 1));

    // Calculate trends
    summary.setOfferCountChange(
        summary.getCurrentMonthOfferCount() - summary.getPreviousMonthOfferCount());
    summary.setOfferCountTrend(calculateTrend(summary.getOfferCountChange()));

    calculateTrendAndPercent(
        summary.getCurrentMonthAveragePrice(),
        summary.getPreviousMonthAveragePrice(),
        summary::setPriceTrend,
        summary::setPriceChangePercent);

    calculateTrendAndPercent(
        summary.getCurrentMonthAveragePricePerSquareMeter(),
        summary.getPreviousMonthAveragePricePerSquareMeter(),
        summary::setPricePerSquareMeterTrend,
        summary::setPricePerSquareMeterChangePercent);

    calculateTrendAndPercent(
        summary.getCurrentMonthAverageSquareMeters(),
        summary.getPreviousMonthAverageSquareMeters(),
        summary::setSizeTrend,
        summary::setSizeChangePercent);

    return summary;
  }

  private String calculateTrend(int difference) {
    if (difference > 0) {
      return "up";
    } else if (difference < 0) {
      return "down";
    }
    return "stable";
  }

  private void calculateTrendAndPercent(
      BigDecimal currentValue,
      BigDecimal previousValue,
      java.util.function.Consumer<String> trendSetter,
      java.util.function.Consumer<BigDecimal> percentSetter) {
    if (currentValue == null || previousValue == null || previousValue.signum() == 0) {
      trendSetter.accept("stable");
      percentSetter.accept(BigDecimal.ZERO);
      return;
    }

    BigDecimal changePercent =
        currentValue
            .subtract(previousValue)
            .divide(previousValue, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

    percentSetter.accept(changePercent);

    if (changePercent.abs().compareTo(BigDecimal.valueOf(2)) < 0) {
      trendSetter.accept("stable");
    } else if (changePercent.signum() > 0) {
      trendSetter.accept("up");
    } else {
      trendSetter.accept("down");
    }
  }
}
