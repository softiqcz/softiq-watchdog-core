package cz.softiq.service.realiq.service.activity;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.entities.UserActivityEntity;
import cz.softiq.service.realiq.model.enums.UserActivityType;
import cz.softiq.service.realiq.model.records.UserActivityItem;
import cz.softiq.service.realiq.repository.UserActivityRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class UserActivityComponent {
  private static final int REMINDER_COOLDOWN_HOURS = 24;

  private final UserActivityRepository userActivityRepository;

  @Transactional(readOnly = true)
  public List<UserActivityItem> getActivities(CoreClientEntity client) {
    return userActivityRepository.findTop50ByClientOrderByCreatedAtDesc(client).stream()
        .map(UserActivityItem::from)
        .toList();
  }

  @Transactional
  public void deleteActivities(CoreClientEntity client) {
    userActivityRepository.deleteAllByClient(client);
  }

  @Transactional
  public void recordTransactionReminder(
      CoreClientEntity client,
      ClientEstate estate,
      Transaction transaction,
      String period,
      String subject,
      String htmlBody) {
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("recipient", estate.getTenantEmail());
    metadata.put("subject", subject);
    metadata.put("estateId", estate.getId());
    metadata.put("estateName", estate.getName());
    metadata.put("transactionId", transaction.getId());
    metadata.put("transactionName", transaction.getName());
    metadata.put("amount", transaction.getAmount());
    metadata.put("period", period);
    metadata.put("htmlBody", htmlBody);
    save(client, UserActivityType.TRANSACTION_REMINDER_EMAIL, metadata);
  }

  @Transactional(readOnly = true)
  public void ensureTransactionReminderAllowed(
      CoreClientEntity client, Long transactionId, String period) {
    LocalDateTime cooldownStart = LocalDateTime.now().minusHours(REMINDER_COOLDOWN_HOURS);
    boolean wasSentRecently =
        userActivityRepository
            .findByClientAndTypeAndCreatedAtAfterOrderByCreatedAtDesc(
                client, UserActivityType.TRANSACTION_REMINDER_EMAIL, cooldownStart)
            .stream()
            .anyMatch(
                activity -> {
                  Map<String, Object> metadata = activity.getMetadata();
                  return String.valueOf(transactionId)
                          .equals(String.valueOf(metadata.get("transactionId")))
                      && period.equals(metadata.get("period"));
                });

    if (wasSentRecently) {
      throw new SoftiqException("Připomínku lze znovu odeslat až po 24 hodinách.");
    }
  }

  @Transactional
  public void recordBankStatementImport(
      CoreClientEntity client,
      ClientEstate estate,
      String fileName,
      String format,
      int transactionCount,
      List<String> periods) {
    String normalizedFileName = fileName == null ? "" : fileName.trim();
    String normalizedFormat = format == null ? "" : format.trim().toUpperCase();
    if (normalizedFileName.isBlank()
        || normalizedFileName.length() > 255
        || !Set.of("CSV", "GPC").contains(normalizedFormat)) {
      throw new SoftiqException("Invalid bank statement import metadata");
    }
    List<String> normalizedPeriods =
        periods == null
            ? List.of()
            : periods.stream()
                .filter(period -> period != null && period.matches("(0[1-9]|1[0-2]) \\d{4}"))
                .distinct()
                .toList();
    Map<String, Object> metadata = new LinkedHashMap<>();
    metadata.put("fileName", normalizedFileName);
    metadata.put("format", normalizedFormat);
    metadata.put("estateId", estate.getId());
    metadata.put("estateName", estate.getName());
    metadata.put("transactionCount", Math.max(1, Math.min(transactionCount, 100_000)));
    metadata.put("periods", new ArrayList<>(normalizedPeriods));
    save(client, UserActivityType.BANK_STATEMENT_IMPORT, metadata);
  }

  public Map<String, Object> extractData(Map<String, Object> requestJson) {
    Object data = requestJson.get("data");
    if (data instanceof Map<?, ?> rawMap) {
      Map<String, Object> result = new LinkedHashMap<>();
      rawMap.forEach((key, value) -> result.put(String.valueOf(key), value));
      return result;
    }
    throw new SoftiqException("Missing or invalid request 'data' object");
  }

  private void save(CoreClientEntity client, UserActivityType type, Map<String, Object> metadata) {
    UserActivityEntity activity = new UserActivityEntity();
    activity.setClient(client);
    activity.setType(type);
    activity.setMetadata(metadata);
    userActivityRepository.save(activity);
  }
}
