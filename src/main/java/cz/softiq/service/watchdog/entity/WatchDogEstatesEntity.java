package cz.softiq.service.watchdog.entity;

import cz.softiq.common.base.model.IBaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "watchdog_estates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchDogEstatesEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "watchdog_id", referencedColumnName = "id")
  private WatchDogEntity watchdogEntity;

  @Column private String origin;
  @Column private String offerId;
  @Column private String header;
  @Column private String address;
  @Column private String price;
  @Column private String squareMeter;
  @Column private String pricePerSquareMeter;
  @Column private Boolean isReported;
  @Column private Boolean isUpdated;

  @Column private Boolean isFavorite;
  @Column private Boolean isHiddenFromClientByAdmin;
  @Column private Boolean isHiddenFromClientByUser;

  @Column(length = 1024)
  private String offerUrl;

  @Column(length = 1024)
  private String mainPicUrl;

  @Column private LocalDate createdAt;
  @Column private BigDecimal softiqIndex;

  @Column(length = 512)
  private String comparisonText;
}
