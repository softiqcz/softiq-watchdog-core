package cz.softiq.service.watchdog.entity;

import cz.softiq.common.base.model.IBaseEntity;
import cz.softiq.core.clients.CoreClientEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "watchdog_payment")
@Data
@NoArgsConstructor
public class PaymentEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne
  @JoinColumn(name = "client_name", referencedColumnName = "clientName")
  private CoreClientEntity client;

  @Column private LocalDateTime createdDate;
  @Column private LocalDateTime updatedDate;
  @Column private String orderId;
  @Column private String orderNumber;
  @Column private BigDecimal amount;
  @Column private String currency;
  @Column private String state;
  @Column private String note;

  public PaymentEntity(
      CoreClientEntity client,
      LocalDateTime createdDate,
      LocalDateTime updatedDate,
      String orderNumber,
      String orderId,
      String currency,
      BigDecimal amount,
      String state,
      String note) {
    this.client = client;
    this.createdDate = createdDate;
    this.updatedDate = updatedDate;
    this.orderNumber = orderNumber;
    this.orderId = orderId;
    this.currency = currency;
    this.amount = amount;
    this.state = state;
    this.note = note;
  }
}
