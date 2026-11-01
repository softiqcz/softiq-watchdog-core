package cz.softiq.service.watchdog.entity;

import cz.softiq.common.base.model.IBaseEntity;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "watchdog_memberships")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WatchDogMembershipEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "client_name", referencedColumnName = "clientName")
  private CoreClientEntity client;

  @Enumerated(EnumType.STRING)
  @Column
  private MembershipEnum membership;

  @Column private BigDecimal agreedPrice;

  @Enumerated(EnumType.STRING)
  @Column
  private PromoCodeEnum promoCode;

  @Enumerated(EnumType.STRING)
  @Column
  private MembershipEnum pendingMembership;

  @Column private String membershipInfo;
  @Column private LocalDateTime validFrom;
  @Column private LocalDateTime validTo;
  @Column private String validToFormatted;
  @Column private Boolean isActive;
  @Column private String uniqueHash;
  @Column private String note;
  @Column private String userName;

  // billing details
  @Column private String idNumber;
  @Column private String subject;
  @Column private String address;
}
