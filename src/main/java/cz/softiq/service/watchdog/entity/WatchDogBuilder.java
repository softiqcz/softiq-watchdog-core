package cz.softiq.service.watchdog.entity;

import cz.softiq.common.base.model.IBaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class WatchDogBuilder implements IBaseEntity {

  private WatchDogEntity watchDogEntity;
  @Builder.Default private Boolean isChangedUrl = false;
  @Builder.Default private Boolean isSettingsSupported = true;
  @Builder.Default private Boolean hasOffers = false;
  @Builder.Default private Boolean isInvalidOffer = false;
  @Builder.Default private WatchDogEstatesEntity offer = null;
}
