package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.service.softiq.model.builder.PublicWatchDogOfferGroup;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchDogOfferService {
  private final WatchDogOfferPresentationComponent offerPresentationComponent;

  String extractPrimaryPriceNumber(String price) {
    return offerPresentationComponent.extractPrimaryPriceNumber(price);
  }

  List<PublicWatchDogOfferGroup> getPublicMostExpensiveWeeklyOffers() {
    return offerPresentationComponent.getPublicMostExpensiveWeeklyOffers();
  }
}
