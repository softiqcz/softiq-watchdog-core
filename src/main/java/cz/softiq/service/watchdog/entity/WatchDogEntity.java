package cz.softiq.service.watchdog.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.softiq.common.base.model.IBaseEntity;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.model.enums.SettingsEnums;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "watchdog")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchDogEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "client_name", referencedColumnName = "clientName")
  @JsonIgnore
  private CoreClientEntity client;

  @Column private Boolean isFirstSyncCompleted;
  @Column private Boolean isVisible;
  @Column private Boolean isEnabled;
  @Column private Integer parentWatchDogId;
  @Column private Boolean doResetSearch;
  @Column private String realityUrl;
  @Column private String estateType;
  @Column private String offerType;
  @Column private String flatCategory;
  @Column private String homeCategory;
  @Column private String landCategory;
  @Column private String comercialCategory;
  @Column private String otherCategory;
  @Column private String exactAddress;
  @Column private String searchAddress;
  @Column private String inRange;
  @Column private String estateCondition;
  @Column private String floorFrom;
  @Column private String floorTo;
  @Column private String usableAreaFrom;
  @Column private String usableAreaTo;
  @Column private String priceFrom;
  @Column private String priceTo;

  // email settings
  @Column private Boolean doEmailReport;

  @Enumerated(EnumType.STRING)
  @Column
  private SettingsEnums.FrequencyEnum emailReportFrequency;

  @Enumerated(EnumType.STRING)
  @Column
  private SettingsEnums.EmailSettingEnum emailReportSetting;

  @Enumerated(EnumType.STRING)
  @Column
  private SettingsEnums.OfferSettingEnum sendOfferAs;

  // URLs
  @Column(columnDefinition = "TEXT")
  private String urlSreality;

  @Column(columnDefinition = "TEXT")
  private String urlBazos;

  @Column(columnDefinition = "TEXT")
  private String urlBezrealitky;

  @Column(columnDefinition = "TEXT")
  private String urlIdnesReality;

  @Column(columnDefinition = "TEXT")
  private String urlRealitymat;

  @Column(columnDefinition = "TEXT")
  private String urlCeskereality;

  // Realiq
  @Enumerated(EnumType.STRING)
  @Column
  private TypeEnum type;

  private Integer realiqId;
}
