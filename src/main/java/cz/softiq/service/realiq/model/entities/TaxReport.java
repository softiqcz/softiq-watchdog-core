package cz.softiq.service.realiq.model.entities;

import cz.softiq.service.realiq.model.enums.TaxRate;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Data
@NoArgsConstructor
public class TaxReport {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "estate_id")
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private ClientEstate estate;

  private Integer year;

  @Column(precision = 15, scale = 2)
  private BigDecimal taxableIncome;

  @Column(precision = 15, scale = 2)
  private BigDecimal deductibleExpenses;

  @Column(precision = 15, scale = 2)
  private BigDecimal otherExpenses;

  @Column(precision = 15, scale = 2)
  private BigDecimal totalExpenses;

  @Column(precision = 15, scale = 2)
  private BigDecimal totalTaxableIncome;

  @Column(precision = 15, scale = 2)
  private BigDecimal totalTax;

  @Enumerated(EnumType.STRING)
  private TaxRate taxRate;

  @Column
  @Temporal(TemporalType.TIMESTAMP)
  private Date updatedAt = new Date();

  public TaxReport(
      ClientEstate estate,
      Integer year,
      BigDecimal taxableIncome,
      BigDecimal deductibleExpenses,
      BigDecimal otherExpenses,
      BigDecimal totalExpenses,
      BigDecimal totalTaxableIncome,
      BigDecimal totalTax,
      TaxRate taxRate) {
    this.estate = estate;
    this.year = year;
    this.taxableIncome = taxableIncome;
    this.deductibleExpenses = deductibleExpenses;
    this.otherExpenses = otherExpenses;
    this.totalExpenses = totalExpenses;
    this.totalTaxableIncome = totalTaxableIncome;
    this.totalTax = totalTax;
    this.taxRate = taxRate;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = new Date();
  }
}
