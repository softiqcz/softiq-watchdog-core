package cz.softiq.service.realiq.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.StringListConverter;
import cz.softiq.service.realiq.model.enums.EstateStatus;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
public class ClientEstate {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "client_id", referencedColumnName = "id")
  @JsonIgnore
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private CoreClientEntity client;

  private String name;
  private String estateType;
  private String flatCategory;
  private String exactAddress;
  private String address;
  private String ownerShip;
  private String estateCondition;

  @Convert(converter = StringListConverter.class)
  @Column(length = 1000)
  private List<String> accessories = new ArrayList<>();

  @Column(precision = 15, scale = 2)
  private BigDecimal usableArea;

  @Column(precision = 15, scale = 2)
  private BigDecimal purchasePrice;

  private String tenantName;
  private String tenantPhone;
  private String tenantEmail;
  private String tenantNote;
  private String managerName;
  private String managerPhone;
  private String managerEmail;
  private String managerNote;

  @Column(length = 1000)
  private String note;

  @Enumerated(EnumType.STRING)
  private EstateStatus status;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateFrom = new Date();

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateTo = new Date();

  @OneToMany(mappedBy = "estate", cascade = CascadeType.ALL)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private List<Transaction> transactions = new ArrayList<>();

  @Column(precision = 15, scale = 2)
  private BigDecimal totalProfit;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedAt = new Date();

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }
}
