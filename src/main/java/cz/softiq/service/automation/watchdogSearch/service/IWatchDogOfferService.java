package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.softiq.model.builder.PublicWatchDogOfferGroup;
import java.util.List;

public interface IWatchDogOfferService extends IBeanProvider, IBaseService {

  default String extractPrimaryPriceNumber(String price) {
    return getWatchDogOfferService().extractPrimaryPriceNumber(price);
  }

  default List<PublicWatchDogOfferGroup> getPublicMostExpensiveWeeklyOffers() {
    return getWatchDogOfferService().getPublicMostExpensiveWeeklyOffers();
  }

  private WatchDogOfferService getWatchDogOfferService() {
    return getBean(WatchDogOfferService.class);
  }
}
