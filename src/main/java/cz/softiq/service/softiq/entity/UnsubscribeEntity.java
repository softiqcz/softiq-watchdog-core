package cz.softiq.service.softiq.entity;

import cz.softiq.common.base.model.IBaseEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "unsubscribe", uniqueConstraints = @UniqueConstraint(columnNames = "clientEmail"))
@Data
@NoArgsConstructor
public class UnsubscribeEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(nullable = false)
  private String clientEmail;

  @Column(nullable = false)
  private LocalDateTime unsubscribeDate;

  public UnsubscribeEntity(String clientEmail, LocalDateTime unsubscribeDate) {
    this.clientEmail = clientEmail;
    this.unsubscribeDate = unsubscribeDate;
  }
}
