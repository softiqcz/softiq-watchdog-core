package cz.softiq.service.watchdog.model.builder;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WatchDogReportBuilder {
  @Builder.Default private Boolean isSuccess = false;

  @Builder.Default private Boolean didSrealityFail = false;
  private String errorMessageSreality;

  @Builder.Default private Boolean didBezrealitkyFail = false;
  private String errorMessageBezrealitky;

  @Builder.Default private Boolean didBazosFail = false;
  private String errorMessageBazos;

  @Builder.Default private Boolean didIdnesFail = false;
  private String errorMessageIdnes;

  @Builder.Default private Boolean didRealitymatFail = false;
  private String errorMessageRealitymat;

  @Builder.Default private Boolean didCeskeRealityFail = false;
  private String errorMessageCeskeReality;
}
