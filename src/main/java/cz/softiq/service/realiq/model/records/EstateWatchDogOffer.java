package cz.softiq.service.realiq.model.records;

import cz.softiq.service.realiq.model.enums.EstateWatchDogResultType;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import java.time.LocalDate;

public record EstateWatchDogOffer(
    Integer id,
    EstateWatchDogResultType analysisType,
    String origin,
    String offerId,
    String header,
    String address,
    String price,
    Boolean isReported,
    Boolean isUpdated,
    Boolean isFavorite,
    Boolean isHiddenFromClientByAdmin,
    Boolean isHiddenFromClientByUser,
    String offerUrl,
    String mainPicUrl,
    LocalDate createdAt) {

  public static EstateWatchDogOffer from(
      WatchDogEstatesEntity offer, EstateWatchDogResultType analysisType) {
    return new EstateWatchDogOffer(
        offer.getId(),
        analysisType,
        offer.getOrigin(),
        offer.getOfferId(),
        offer.getHeader(),
        offer.getAddress(),
        offer.getPrice(),
        offer.getIsReported(),
        offer.getIsUpdated(),
        offer.getIsFavorite(),
        offer.getIsHiddenFromClientByAdmin(),
        offer.getIsHiddenFromClientByUser(),
        offer.getOfferUrl(),
        offer.getMainPicUrl(),
        offer.getCreatedAt());
  }
}
