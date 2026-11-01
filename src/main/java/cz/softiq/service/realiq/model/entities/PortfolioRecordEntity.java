package cz.softiq.service.realiq.model.entities;

import cz.softiq.core.clients.CoreClientEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
    name = "portfolio_record",
    indexes = {
      @Index(name = "idx_portfolio_record_client_estate", columnList = "client_name,estate_id"),
      @Index(name = "idx_portfolio_record_created", columnList = "created_at")
    })
@Data
@NoArgsConstructor
public class PortfolioRecordEntity {

  @Id private String id;

  @ManyToOne(optional = false)
  @JoinColumn(name = "client_name", referencedColumnName = "clientName", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private CoreClientEntity client;

  @ManyToOne(optional = false)
  @JoinColumn(name = "estate_id", nullable = false)
  @OnDelete(action = OnDeleteAction.CASCADE)
  private ClientEstate estate;

  @Column(nullable = false, length = 255)
  private String title;

  @Column(nullable = false, length = 40)
  private String category = "OTHER";

  @Column(length = 4000)
  private String note;

  @Column(name = "important_date")
  private LocalDate importantDate;

  @Column(name = "repeat_interval")
  private Integer repeatInterval;

  @Column(name = "repeat_unit", length = 16)
  private String repeatUnit;

  @Column(name = "reminder_amount")
  private Integer reminderAmount;

  @Column(name = "reminder_unit", length = 16)
  private String reminderUnit;

  @Column(name = "last_reminder_sent_date")
  private LocalDate lastReminderSentDate;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(columnDefinition = "json", nullable = false)
  private List<Map<String, Object>> documents = new ArrayList<>();

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt = LocalDateTime.now();
}
