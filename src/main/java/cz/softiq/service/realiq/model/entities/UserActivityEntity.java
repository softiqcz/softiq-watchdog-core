package cz.softiq.service.realiq.model.entities;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.enums.UserActivityType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "user_activity",
    indexes =
        @Index(name = "idx_user_activity_client_created", columnList = "client_name,created_at"))
@Data
@NoArgsConstructor
public class UserActivityEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "client_name", referencedColumnName = "clientName", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private CoreClientEntity client;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 64)
  private UserActivityType type;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json", nullable = false)
  private Map<String, Object> metadata = new LinkedHashMap<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
