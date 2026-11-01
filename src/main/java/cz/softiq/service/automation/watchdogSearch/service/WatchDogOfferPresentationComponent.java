package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.softiq.model.builder.PublicWatchDogOffer;
import cz.softiq.service.softiq.model.builder.PublicWatchDogOfferGroup;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import cz.softiq.service.watchdog.repository.WatchDogRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WatchDogOfferPresentationComponent {
  private static final Pattern FIRST_PRICE_PATTERN = Pattern.compile("\\d[\\d\\s\\u00A0.,]*");
  private static final Pattern PUBLIC_PRICE_PATTERN = Pattern.compile("-?\\d[\\d\\s\\u00A0.,]*");
  private static final ZoneId PRAGUE_ZONE = ZoneId.of("Europe/Prague");
  private static final List<PublicWatchDogDefinition> PUBLIC_SALE_WATCHDOGS =
      List.of(
          new PublicWatchDogDefinition(667, "Domy"),
          new PublicWatchDogDefinition(737, "Byty"),
          new PublicWatchDogDefinition(2, "Byty"));
  private static final int PUBLIC_RESULTS_LIMIT = 5;

  private final WatchDogRepository watchDogRepository;
  private final WatchDogEstatesRepository watchDogEstatesRepository;

  private record PublicWatchDogDefinition(Integer id, String fallbackEstateType) {}

  private record PricedPublicOffer(WatchDogEstatesEntity offer, BigDecimal price) {}

  public String extractPrimaryPriceNumber(String rawPrice) {
    if (rawPrice == null || rawPrice.isBlank()) {
      return rawPrice;
    }

    Matcher matcher = FIRST_PRICE_PATTERN.matcher(rawPrice);
    if (!matcher.find()) {
      return rawPrice;
    }

    String number = matcher.group().replaceAll("[^0-9]", "");
    if (number.isEmpty()) {
      return rawPrice;
    }

    BigDecimal value = new BigDecimal(number);
    NumberFormat formatter = NumberFormat.getInstance(Locale.forLanguageTag("cs-CZ"));
    return formatter.format(value) + " Kč";
  }

  public List<PublicWatchDogOfferGroup> getPublicMostExpensiveWeeklyOffers() {
    LocalDate today = LocalDate.now(PRAGUE_ZONE);
    return buildPublicMostExpensiveWeeklyOffers(today.minusDays(6), today.plusDays(1));
  }

  public List<PublicWatchDogOfferGroup> buildPublicMostExpensiveWeeklyOffers(
      LocalDate start, LocalDate end) {
    return buildMostExpensiveWeeklyOffers(PUBLIC_SALE_WATCHDOGS, start, end);
  }

  private List<PublicWatchDogOfferGroup> buildMostExpensiveWeeklyOffers(
      List<PublicWatchDogDefinition> publicWatchDogs, LocalDate start, LocalDate end) {
    return publicWatchDogs.stream()
        .map(publicWatchDog -> buildOfferGroup(publicWatchDog, start, end))
        .toList();
  }

  private PublicWatchDogOfferGroup buildOfferGroup(
      PublicWatchDogDefinition publicWatchDog, LocalDate start, LocalDate end) {
    Integer watchDogId = publicWatchDog.id();
    WatchDogEntity watchDog = watchDogRepository.findById(watchDogId).orElse(null);
    String estateType =
        watchDog == null ? publicWatchDog.fallbackEstateType() : watchDog.getEstateType();
    String offerType = watchDog == null ? "Prodej" : watchDog.getOfferType();
    String searchAddress = watchDog == null ? "Česko" : watchDog.getSearchAddress();
    List<WatchDogEstatesEntity> weeklyOffers =
        watchDogEstatesRepository.findWatchDogEstatesEntitiesToShowBetween(watchDog, start, end);
    List<PublicWatchDogOffer> offers = getMostExpensiveOffers(weeklyOffers);
    Set<Integer> offerIds =
        offers.stream().map(PublicWatchDogOffer::id).collect(Collectors.toSet());
    List<PublicWatchDogOffer> favoriteOffers = getAdditionalFavoriteOffers(weeklyOffers, offerIds);
    return new PublicWatchDogOfferGroup(
        watchDogId, estateType, offerType, searchAddress, offers, favoriteOffers);
  }

  private List<PublicWatchDogOffer> getMostExpensiveOffers(
      List<WatchDogEstatesEntity> weeklyOffers) {
    return weeklyOffers.stream()
        .map(offer -> new PricedPublicOffer(offer, parsePositivePrice(offer.getPrice())))
        .filter(entry -> entry.price() != null)
        .sorted(
            Comparator.comparing(PricedPublicOffer::price)
                .reversed()
                .thenComparing(entry -> entry.offer().getId(), Comparator.reverseOrder()))
        .limit(PUBLIC_RESULTS_LIMIT)
        .map(entry -> toPublicWatchDogOffer(entry.offer()))
        .toList();
  }

  private List<PublicWatchDogOffer> getAdditionalFavoriteOffers(
      List<WatchDogEstatesEntity> weeklyOffers, Set<Integer> excludedOfferIds) {
    return weeklyOffers.stream()
        .filter(offer -> Boolean.TRUE.equals(offer.getIsFavorite()))
        .filter(offer -> !excludedOfferIds.contains(offer.getId()))
        .sorted(
            Comparator.comparing(WatchDogEstatesEntity::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(WatchDogEstatesEntity::getId, Comparator.reverseOrder()))
        .map(this::toPublicWatchDogOffer)
        .toList();
  }

  private BigDecimal parsePositivePrice(String rawPrice) {
    if (rawPrice == null || rawPrice.isBlank()) {
      return null;
    }

    Matcher matcher = PUBLIC_PRICE_PATTERN.matcher(rawPrice);
    if (!matcher.find() || matcher.group().startsWith("-")) {
      return null;
    }

    String digits = matcher.group().replaceAll("[^0-9]", "");
    if (digits.isEmpty()) {
      return null;
    }

    BigDecimal price = new BigDecimal(digits);
    return price.signum() > 0 ? price : null;
  }

  private PublicWatchDogOffer toPublicWatchDogOffer(WatchDogEstatesEntity offer) {
    return new PublicWatchDogOffer(
        offer.getId(),
        offer.getCreatedAt(),
        offer.getHeader(),
        offer.getAddress(),
        offer.getPrice(),
        offer.getMainPicUrl(),
        offer.getOrigin(),
        offer.getOfferUrl());
  }
}
