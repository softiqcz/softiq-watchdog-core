package cz.softiq.service.softiq.model.builder;

import java.time.LocalDate;

public record PublicWatchDogOffer(
    Integer id,
    LocalDate createdAt,
    String header,
    String address,
    String price,
    String mainPicUrl,
    String origin,
    String offerUrl) {}
