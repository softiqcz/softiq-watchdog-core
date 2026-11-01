package cz.softiq.service.softiq.entity;

import cz.softiq.common.base.model.IBaseEntity;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.softiq.model.enums.PromoEmailType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "promo_emails",
    indexes = {
      @Index(
          name = "idx_promo_emails_client_type_sent_at",
          columnList = "client_id,email_type,sent_at")
    })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PromoEmailEntity implements IBaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "client_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private CoreClientEntity client;

  @Enumerated(EnumType.STRING)
  @Column(name = "email_type", nullable = false, length = 64)
  private PromoEmailType emailType;

  @Column(name = "sent_at", nullable = false)
  private LocalDateTime sentAt;
}
