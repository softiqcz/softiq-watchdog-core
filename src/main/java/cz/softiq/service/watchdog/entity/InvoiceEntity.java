package cz.softiq.service.watchdog.entity;

import cz.softiq.common.base.model.IBaseEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "invoice")
@Data
@NoArgsConstructor
public class InvoiceEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column private LocalDate createdDate;
  @Column private String clientName;
  @Column private String clientAddress;
  @Column private String clientIdNumber;
  @Column private Integer invoiceNumber;
  @Column private BigDecimal totalPrice;
  @Column private String note;
  @Lob private byte[] pdfContent;

  public InvoiceEntity(
      LocalDate createdDate,
      String clientName,
      String clientAddress,
      String clientIdNumber,
      Integer invoiceNumber,
      BigDecimal totalPrice,
      String note,
      byte[] pdfContent) {
    this.createdDate = createdDate;
    this.clientName = clientName;
    this.clientAddress = clientAddress;
    this.clientIdNumber = clientIdNumber;
    this.invoiceNumber = invoiceNumber;
    this.totalPrice = totalPrice;
    this.note = note;
    this.pdfContent = pdfContent;
  }
}
