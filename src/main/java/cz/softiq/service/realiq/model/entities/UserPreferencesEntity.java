package cz.softiq.service.realiq.model.entities;

import cz.softiq.core.clients.CoreClientEntity;
import jakarta.persistence.*;
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
    name = "user_preferences",
    uniqueConstraints = @UniqueConstraint(columnNames = "client_name"))
@Data
@NoArgsConstructor
public class UserPreferencesEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(optional = false)
  @JoinColumn(name = "client_name", referencedColumnName = "clientName", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private CoreClientEntity client;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json", nullable = false)
  private Map<String, Object> preferences = new LinkedHashMap<>();
}
