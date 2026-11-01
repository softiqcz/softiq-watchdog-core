package cz.softiq.service.realiq.model.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import cz.softiq.service.realiq.model.TransactionVerification;
import cz.softiq.service.realiq.model.enums.Recurrence;
import cz.softiq.service.realiq.model.enums.TransactionType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Data
public class Transaction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String name;
  private BigDecimal amount;
  private boolean isTaxDeductible;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateFrom;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date dateTo;

  @Enumerated(EnumType.STRING)
  private TransactionType transactionType;

  @Enumerated(EnumType.STRING)
  private Recurrence recurrence;

  private Boolean showInDashboard;

  @Column(length = 64)
  private String bankImportId;

  @Column(length = 64)
  private String bankImportSeriesId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json")
  private TransactionVerification verification = new TransactionVerification();

  @ManyToOne
  @JoinColumn(name = "estate_id")
  @JsonIgnore
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private ClientEstate estate;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedAt = new Date();

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }
}
