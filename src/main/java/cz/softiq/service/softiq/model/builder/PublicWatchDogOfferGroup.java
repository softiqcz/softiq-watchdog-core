package cz.softiq.service.softiq.model.builder;

import java.util.List;

public record PublicWatchDogOfferGroup(
    Integer watchdogId,
    String estateType,
    String offerType,
    String searchAddress,
    List<PublicWatchDogOffer> offers,
    List<PublicWatchDogOffer> favoriteOffers) {}
