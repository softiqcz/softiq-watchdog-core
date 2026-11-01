package cz.softiq.service.realiq.service.portfolio;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.PortfolioRecordEntity;
import cz.softiq.service.realiq.model.enums.ActionEnum;
import cz.softiq.service.realiq.repository.PortfolioRecordRepository;
import cz.softiq.service.realiq.service.email.IRealiqEmailService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
class PortfolioRecordComponent implements IRealiqEmailService {
  private static final int MAX_DOCUMENT_SIZE = 10 * 1024 * 1024;
  private static final int MAX_DOCUMENTS = 10;
  private static final int MAX_DOCUMENTS_PER_USER = 5;
  private static final String DOCUMENT_LIMIT_MESSAGE =
      "Tato funkce je stále ve vývoji. Pokud jste dosáhli maximálního počtu 5 souborů, "
          + "pošlete nám prosím požadavek, abychom její vývoj urychlili.";
  private static final Set<String> CATEGORIES =
      Set.of("INSURANCE", "LEASE", "TAX", "INSPECTION", "WARRANTY", "OTHER");
  private static final Set<String> REPEAT_UNITS = Set.of("MONTH", "YEAR");
  private static final Set<String> REMINDER_UNITS = Set.of("DAY", "MONTH");

  private final PortfolioRecordRepository portfolioRecordRepository;

  @Transactional(readOnly = true)
  public List<Map<String, Object>> getRecords(CoreClientEntity client, ClientEstate estate) {
    verifyEstate(client, estate);
    return portfolioRecordRepository.findAllByClientAndEstate(client, estate).stream()
        .sorted(Comparator.comparing(PortfolioRecordEntity::getCreatedAt).reversed())
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<String, Object> getDocumentQuota(CoreClientEntity client) {
    int documentCount = getDocumentCount(client);
    Map<String, Object> quota = new LinkedHashMap<>();
    quota.put("count", documentCount);
    quota.put("maximum", MAX_DOCUMENTS_PER_USER);
    quota.put("limitReached", documentCount >= MAX_DOCUMENTS_PER_USER);
    return quota;
  }

  @Transactional
  public List<Map<String, Object>> doActionWithRecords(
      List<Map<String, Object>> records,
      CoreClientEntity client,
      ClientEstate estate,
      ActionEnum fallbackAction) {
    verifyEstate(client, estate);
    List<Map<String, Object>> result = new ArrayList<>();
    for (Map<String, Object> record : records) {
      ActionEnum action =
          record.get("type") == null ? fallbackAction : parseAction(record.get("type"));
      if (action == null) {
        throw new SoftiqException("Missing record action");
      }
      if (action.equals(ActionEnum.DELETE)) {
        deleteRecord(record, client, estate);
      } else {
        result.add(toResponse(saveRecord(record, client, estate, action)));
      }
    }
    return result;
  }

  public List<Map<String, Object>> extractRecords(Map<String, Object> data) {
    if (data.get("records") instanceof List<?>) {
      return extractItems(data.get("records"), "records");
    }
    Object recordObject = data.get("record") instanceof Map<?, ?> ? data.get("record") : data;
    Map<?, ?> rawRecord = (Map<?, ?>) recordObject;
    Map<String, Object> record = new LinkedHashMap<>();
    rawRecord.forEach((key, value) -> record.put(String.valueOf(key), value));
    return List.of(record);
  }

  private List<Map<String, Object>> extractItems(Object itemsObject, String fieldName) {
    if (!(itemsObject instanceof List<?> rawItems)) {
      throw new SoftiqException("Missing or invalid '" + fieldName + "' list");
    }
    List<Map<String, Object>> items = new ArrayList<>();
    for (Object itemObject : rawItems) {
      if (!(itemObject instanceof Map<?, ?> rawItem)) {
        throw new SoftiqException("Invalid " + fieldName + " item format");
      }
      Map<String, Object> item = new LinkedHashMap<>();
      rawItem.forEach((key, value) -> item.put(String.valueOf(key), value));
      items.add(item);
    }
    return items;
  }

  private PortfolioRecordEntity saveRecord(
      Map<String, Object> record, CoreClientEntity client, ClientEstate estate, ActionEnum action) {
    PortfolioRecordEntity entity;
    int previousRecordDocumentCount = 0;
    if (action.equals(ActionEnum.ADD)) {
      entity = new PortfolioRecordEntity();
      entity.setId(normalizeId(record.get("id")));
      entity.setClient(client);
      entity.setEstate(estate);
      entity.setCreatedAt(parseCreatedAt(record.get("createdAt")));
    } else if (action.equals(ActionEnum.EDIT)) {
      entity = getExistingRecord(record, client, estate);
      previousRecordDocumentCount = entity.getDocuments().size();
    } else {
      throw new SoftiqException("Unsupported record action");
    }

    int currentDocumentCount = getDocumentCount(client);
    setRecordValues(entity, record);
    int prospectiveDocumentCount =
        currentDocumentCount - previousRecordDocumentCount + entity.getDocuments().size();
    if (prospectiveDocumentCount > MAX_DOCUMENTS_PER_USER
        && prospectiveDocumentCount > currentDocumentCount) {
      throw new SoftiqException(DOCUMENT_LIMIT_MESSAGE);
    }
    return portfolioRecordRepository.save(entity);
  }

  private int getDocumentCount(CoreClientEntity client) {
    return Math.toIntExact(
        portfolioRecordRepository.countDocumentsByClientName(client.getClientName()));
  }

  private void deleteRecord(
      Map<String, Object> record, CoreClientEntity client, ClientEstate estate) {
    PortfolioRecordEntity entity = getExistingRecord(record, client, estate);
    portfolioRecordRepository.delete(entity);
  }

  private PortfolioRecordEntity getExistingRecord(
      Map<String, Object> record, CoreClientEntity client, ClientEstate estate) {
    String id = record.get("id") == null ? null : record.get("id").toString();
    if (id == null || id.isBlank()) {
      throw new SoftiqException("Missing record ID");
    }
    PortfolioRecordEntity entity =
        portfolioRecordRepository
            .findByIdAndClient(id, client)
            .orElseThrow(() -> new SoftiqException("Record not found"));
    if (!entity.getEstate().getId().equals(estate.getId())) {
      throw new SoftiqException("Record not found");
    }
    return entity;
  }

  private void setRecordValues(PortfolioRecordEntity entity, Map<String, Object> record) {
    String title = normalizeString(record.get("title"), 255);
    if (title.isBlank()) {
      throw new SoftiqException("Missing record title");
    }

    LocalDate importantDate = parseDate(record.get("importantDate"));
    List<Map<String, Object>> documents = normalizeDocuments(record.get("documents"));
    if (importantDate == null && documents.isEmpty()) {
      throw new SoftiqException("Record needs a date or document");
    }

    entity.setTitle(title);
    entity.setCategory(normalizeCategory(record.get("category")));
    entity.setNote(normalizeString(record.get("note"), 4000));
    entity.setImportantDate(importantDate);
    entity.setRepeatInterval(normalizeRepeatInterval(record.get("repeatInterval"), importantDate));
    entity.setRepeatUnit(normalizeRepeatUnit(record.get("repeatUnit"), entity.getRepeatInterval()));
    entity.setReminderAmount(normalizeReminderAmount(record.get("reminderAmount"), importantDate));
    entity.setReminderUnit(
        normalizeReminderUnit(record.get("reminderUnit"), entity.getReminderAmount()));
    entity.setDocuments(documents);
  }

  @Transactional
  public void sendDueRecordReminders() {
    LocalDate today = LocalDate.now();
    portfolioRecordRepository.findAllByReminderAmountIsNotNullAndImportantDateIsNotNull().stream()
        .filter(record -> record.getReminderUnit() != null)
        .forEach(record -> sendRecordReminderIfDue(record, today));
  }

  private void sendRecordReminderIfDue(PortfolioRecordEntity record, LocalDate today) {
    LocalDate occurrence = getUpcomingOccurrence(record, today);
    if (occurrence == null || occurrence.equals(record.getLastReminderSentDate())) {
      return;
    }

    LocalDate reminderDate =
        "MONTH".equals(record.getReminderUnit())
            ? occurrence.minusMonths(record.getReminderAmount())
            : occurrence.minusDays(record.getReminderAmount());
    if (!today.equals(reminderDate)) {
      return;
    }

    portfolioRecordReminder(record, occurrence);
    record.setLastReminderSentDate(occurrence);
    portfolioRecordRepository.save(record);
  }

  private LocalDate getUpcomingOccurrence(PortfolioRecordEntity record, LocalDate today) {
    LocalDate base = record.getImportantDate();
    if (base == null || record.getReminderAmount() == null) {
      return null;
    }
    if (record.getRepeatInterval() == null
        || record.getRepeatUnit() == null
        || !base.isBefore(today)) {
      return base;
    }

    long elapsed =
        "YEAR".equals(record.getRepeatUnit())
            ? ChronoUnit.YEARS.between(base, today)
            : ChronoUnit.MONTHS.between(base.withDayOfMonth(1), today.withDayOfMonth(1));
    long occurrenceOffset =
        Math.max(
            record.getRepeatInterval(),
            (elapsed / record.getRepeatInterval()) * record.getRepeatInterval());
    LocalDate occurrence = addOccurrence(base, occurrenceOffset, record.getRepeatUnit());
    while (occurrence.isBefore(today)) {
      occurrenceOffset += record.getRepeatInterval();
      occurrence = addOccurrence(base, occurrenceOffset, record.getRepeatUnit());
    }
    return occurrence;
  }

  private LocalDate addOccurrence(LocalDate base, long count, String unit) {
    return "YEAR".equals(unit) ? base.plusYears(count) : base.plusMonths(count);
  }

  private ActionEnum parseAction(Object value) {
    if (value == null || value.toString().isBlank()) {
      throw new SoftiqException("Missing record action");
    }
    try {
      return ActionEnum.valueOf(value.toString());
    } catch (IllegalArgumentException exception) {
      throw new SoftiqException("Invalid record action");
    }
  }

  private void verifyEstate(CoreClientEntity client, ClientEstate estate) {
    if (estate == null || !client.equals(estate.getClient())) {
      throw new SoftiqException("Estate not found");
    }
  }

  private String normalizeId(Object value) {
    String id = value == null ? "" : value.toString().trim();
    return id.isBlank() ? UUID.randomUUID().toString() : normalizeString(id, 80);
  }

  private String normalizeString(Object value, int maxLength) {
    String normalized = value == null ? "" : value.toString().trim();
    return normalized.length() > maxLength ? normalized.substring(0, maxLength) : normalized;
  }

  private String normalizeCategory(Object value) {
    String category = value == null ? "OTHER" : value.toString().trim();
    return CATEGORIES.contains(category) ? category : "OTHER";
  }

  private LocalDate parseDate(Object value) {
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    try {
      return LocalDate.parse(value.toString().substring(0, 10));
    } catch (DateTimeParseException | StringIndexOutOfBoundsException exception) {
      throw new SoftiqException("Invalid record date");
    }
  }

  private LocalDateTime parseCreatedAt(Object value) {
    if (value == null || value.toString().isBlank()) {
      return LocalDateTime.now();
    }
    try {
      return LocalDateTime.parse(value.toString().replace("Z", ""));
    } catch (DateTimeParseException exception) {
      return LocalDateTime.now();
    }
  }

  private Integer normalizeRepeatInterval(Object value, LocalDate importantDate) {
    if (importantDate == null || !(value instanceof Number number)) {
      return null;
    }
    return Math.max(1, number.intValue());
  }

  private String normalizeRepeatUnit(Object value, Integer repeatInterval) {
    if (repeatInterval == null || value == null) {
      return null;
    }
    String unit = value.toString().trim();
    if (!REPEAT_UNITS.contains(unit)) {
      throw new SoftiqException("Invalid repeat unit");
    }
    return unit;
  }

  private Integer normalizeReminderAmount(Object value, LocalDate importantDate) {
    if (importantDate == null || !(value instanceof Number number)) {
      return null;
    }
    return Math.max(1, number.intValue());
  }

  private String normalizeReminderUnit(Object value, Integer reminderAmount) {
    if (reminderAmount == null || value == null) {
      return null;
    }
    String unit = value.toString().trim();
    if (!REMINDER_UNITS.contains(unit)) {
      throw new SoftiqException("Invalid reminder unit");
    }
    return unit;
  }

  private List<Map<String, Object>> normalizeDocuments(Object value) {
    if (!(value instanceof List<?> rawDocuments)) {
      return new ArrayList<>();
    }
    if (rawDocuments.size() > MAX_DOCUMENTS) {
      throw new SoftiqException("Too many documents");
    }

    List<Map<String, Object>> documents = new ArrayList<>();
    for (Object rawDocument : rawDocuments) {
      if (!(rawDocument instanceof Map<?, ?> document)) {
        throw new SoftiqException("Invalid document data");
      }
      Map<String, Object> normalized = new LinkedHashMap<>();
      String id = normalizeId(document.get("id"));
      String name = normalizeString(document.get("name"), 255);
      String type = normalizeString(document.get("type"), 120);
      Number size = document.get("size") instanceof Number number ? number : null;
      if (name.isBlank() || size == null || size.longValue() < 0) {
        throw new SoftiqException("Invalid document data");
      }
      if (size.longValue() > MAX_DOCUMENT_SIZE) {
        throw new SoftiqException("Document is too large");
      }

      normalized.put("id", id);
      normalized.put("name", name);
      normalized.put("size", size.longValue());
      normalized.put("type", type);
      normalized.put(
          "contentBase64", normalizeString(document.get("contentBase64"), Integer.MAX_VALUE));
      documents.add(normalized);
    }
    return documents;
  }

  private Map<String, Object> toResponse(PortfolioRecordEntity entity) {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("id", entity.getId());
    response.put("estateId", entity.getEstate().getId());
    response.put("title", entity.getTitle());
    response.put("category", entity.getCategory());
    response.put("note", entity.getNote());
    response.put(
        "importantDate",
        entity.getImportantDate() == null ? null : entity.getImportantDate().toString());
    response.put("repeatInterval", entity.getRepeatInterval());
    response.put("repeatUnit", entity.getRepeatUnit());
    response.put("reminderAmount", entity.getReminderAmount());
    response.put("reminderUnit", entity.getReminderUnit());
    response.put("documents", entity.getDocuments());
    response.put("createdAt", entity.getCreatedAt().toString());
    return response;
  }
}
