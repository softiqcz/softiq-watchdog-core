package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class WatchDogOfferUtils {
  private WatchDogOfferUtils() {}

  private static final int DUPLICATE_RETENTION_DAYS = 3;
  private static final Locale CZECH_LOCALE = Locale.forLanguageTag("cs-CZ");
  private static final Set<String> CZECH_COUNTRY_NAMES =
      Set.of("cesko", "ceska republika", "czechia", "czech republic");
  private static final Set<String> FOREIGN_HEADER_PREFIXES =
      Set.of("more", "mori", "morsk", "plaz");
  private static final Set<String> AREA_UNIT_TOKENS = Set.of("m2", "m²");
  private static final Pattern SQUARE_METER_PATTERN =
      Pattern.compile("(?<![\\d.,])(\\d+(?:[.,]\\d+)?)\\h*m(?:²|2)(?![\\p{L}\\p{N}])");
  private static final Set<String> COUNTRY_NAMES = createCountryNames();

  public static String normalizeSrealityOfferId(String offerId) {
    if (offerId == null) {
      return null;
    }
    String numericOfferId = offerId.replaceFirst("^(estate-list-item-|estate-|list-item-)", "");
    if (numericOfferId.isBlank()) {
      return offerId;
    }
    return "estate-list-item-" + numericOfferId;
  }

  public static Optional<String> extractSquareMeter(String header) {
    if (header == null || header.isBlank()) {
      return Optional.empty();
    }

    Matcher matcher = SQUARE_METER_PATTERN.matcher(header);
    if (!matcher.find()) {
      return Optional.empty();
    }

    BigDecimal squareMeter = new BigDecimal(matcher.group(1).replace(',', '.'));
    if (squareMeter.signum() <= 0) {
      return Optional.empty();
    }
    return Optional.of(squareMeter.stripTrailingZeros().toPlainString());
  }

  public static Optional<String> calculatePricePerSquareMeter(String price, String squareMeter) {
    if (price == null || price.isBlank() || squareMeter == null || squareMeter.isBlank()) {
      return Optional.empty();
    }

    String priceDigits = price.replaceAll("[^0-9]", "");
    if (priceDigits.isEmpty()) {
      return Optional.empty();
    }

    try {
      BigDecimal numericPrice = new BigDecimal(priceDigits);
      BigDecimal numericSquareMeter = new BigDecimal(squareMeter.replace(',', '.'));
      if (numericPrice.signum() <= 0 || numericSquareMeter.signum() <= 0) {
        return Optional.empty();
      }
      return Optional.of(
          numericPrice.divide(numericSquareMeter, 0, RoundingMode.HALF_UP).toPlainString());
    } catch (NumberFormatException e) {
      return Optional.empty();
    }
  }

  public static void populateMissingAreaAndPricePerSquareMeter(WatchDogEstatesEntity offer) {
    if (offer == null) {
      return;
    }

    String squareMeter = offer.getSquareMeter();
    if (squareMeter == null || squareMeter.isBlank()) {
      squareMeter = extractSquareMeter(offer.getHeader()).orElse(null);
      offer.setSquareMeter(squareMeter);
    }

    if (offer.getPricePerSquareMeter() == null || offer.getPricePerSquareMeter().isBlank()) {
      offer.setPricePerSquareMeter(
          calculatePricePerSquareMeter(offer.getPrice(), squareMeter).orElse(null));
    }
  }

  public static Optional<String> findForeignCountry(String address) {
    if (address == null || address.isBlank()) {
      return Optional.empty();
    }

    String[] addressParts = address.split(",");
    String country = addressParts[addressParts.length - 1].trim();
    String normalizedCountry = normalizeText(country);
    if (!COUNTRY_NAMES.contains(normalizedCountry)
        || CZECH_COUNTRY_NAMES.contains(normalizedCountry)) {
      return Optional.empty();
    }
    return Optional.of(country);
  }

  public static Optional<String> findForeignHeaderKeyword(String header) {
    if (header == null || header.isBlank()) {
      return Optional.empty();
    }

    return Arrays.stream(normalizeText(header).split(" "))
        .filter(
            word -> FOREIGN_HEADER_PREFIXES.stream().anyMatch(prefix -> word.startsWith(prefix)))
        .findFirst();
  }

  private static Set<String> createCountryNames() {
    return Arrays.stream(Locale.getISOCountries())
        .flatMap(
            countryCode -> {
              Locale countryLocale = Locale.of("", countryCode);
              return Stream.of(
                  countryLocale.getDisplayCountry(CZECH_LOCALE),
                  countryLocale.getDisplayCountry(Locale.ENGLISH));
            })
        .map(WatchDogOfferUtils::normalizeText)
        .filter(countryName -> !countryName.isBlank())
        .collect(Collectors.toUnmodifiableSet());
  }

  private static String normalizeText(String value) {
    return Normalizer.normalize(value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^\\p{L}\\p{N}]+", " ")
        .trim();
  }

  public static boolean isDuplicate(
      WatchDogEstatesEntity newOffer, WatchDogEstatesEntity existingOffer) {
    return isDuplicate(newOffer, existingOffer, LocalDate.now());
  }

  public static boolean isDuplicate(
      WatchDogEstatesEntity newOffer,
      WatchDogEstatesEntity existingOffer,
      LocalDate referenceDate) {
    return isWithinDuplicateRetention(existingOffer, referenceDate)
        && Objects.equals(newOffer.getPrice(), existingOffer.getPrice())
        && Objects.equals(
            normalizeOfferHeader(newOffer.getHeader()),
            normalizeOfferHeader(existingOffer.getHeader()))
        && Objects.equals(
            normalizeOfferAddress(newOffer.getAddress()),
            normalizeOfferAddress(existingOffer.getAddress()));
  }

  private static boolean isWithinDuplicateRetention(
      WatchDogEstatesEntity offer, LocalDate referenceDate) {
    LocalDate createdAt = offer.getCreatedAt();
    return createdAt != null
        && !createdAt.isBefore(referenceDate.minusDays(DUPLICATE_RETENTION_DAYS));
  }

  private static String normalizeOfferHeader(String header) {
    if (header == null) {
      return null;
    }
    return Arrays.stream(normalizeText(header).split(" "))
        .filter(token -> !AREA_UNIT_TOKENS.contains(token))
        .collect(Collectors.joining(" "));
  }

  private static String normalizeOfferAddress(String address) {
    if (address == null) {
      return null;
    }
    return Arrays.stream(normalizeText(address).split(" "))
        .distinct()
        .sorted()
        .collect(Collectors.joining(" "));
  }
}
