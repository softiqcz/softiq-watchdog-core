package cz.softiq.service.realiq.service.bankstatement;

import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.Recurrence;
import cz.softiq.service.realiq.model.records.BankStatementImportResult;
import cz.softiq.service.realiq.model.records.BankStatementImportResult.BankStatementTransaction;
import cz.softiq.service.realiq.model.records.BankStatementImportResult.ExistingRecurringTransaction;
import cz.softiq.service.realiq.repository.TransactionRepository;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
class BankStatementImportComponent {
  private static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
  private static final Pattern ACCOUNT_NUMBER_PATTERN = Pattern.compile("([0-9-]+/[0-9]{4})");
  private static final DateTimeFormatter CSV_DATE_FORMAT = DateTimeFormatter.ofPattern("d.M.uuuu");
  private static final java.nio.charset.Charset WINDOWS_1250 =
      java.nio.charset.Charset.forName("windows-1250");

  private final TransactionRepository transactionRepository;

  BankStatementImportComponent(TransactionRepository transactionRepository) {
    this.transactionRepository = transactionRepository;
  }

  public BankStatementImportResult parse(String fileName, String base64Content, Long estateId) {
    if (fileName == null || fileName.isBlank()) {
      throw new SoftiqException("Missing bank statement file name");
    }
    if (base64Content == null || base64Content.isBlank()) {
      throw new SoftiqException("The bank statement file is empty");
    }
    int maximumBase64Length = ((MAX_FILE_SIZE_BYTES + 2) / 3) * 4;
    if (base64Content.length() > maximumBase64Length) {
      throw new SoftiqException("Bank statement files cannot exceed 5 MB");
    }

    byte[] bytes;
    try {
      bytes = Base64.getDecoder().decode(base64Content);
    } catch (IllegalArgumentException exception) {
      throw new SoftiqException("Invalid bank statement file content");
    }
    if (bytes.length == 0 || bytes.length > MAX_FILE_SIZE_BYTES) {
      throw new SoftiqException("Bank statement files must be between 1 byte and 5 MB");
    }

    String normalizedFileName = fileName.toLowerCase(Locale.ROOT);
    List<BankStatementTransaction> parsedTransactions;
    String format;
    if (normalizedFileName.endsWith(".csv")) {
      format = "CSV";
      parsedTransactions = parseCsv(bytes);
    } else if (normalizedFileName.endsWith(".gpc")) {
      format = "GPC";
      parsedTransactions = parseGpc(bytes);
    } else {
      throw new SoftiqException("Only CSV and GPC bank statements are supported");
    }

    if (parsedTransactions.isEmpty()) {
      throw new SoftiqException("No transactions were found in the bank statement");
    }

    Set<String> importIds = new HashSet<>();
    parsedTransactions.forEach(transaction -> importIds.add(transaction.importId()));
    Set<String> alreadyImportedIds =
        transactionRepository.findAllByEstateIdAndBankImportIdIn(estateId, importIds).stream()
            .map(transaction -> transaction.getBankImportId())
            .collect(java.util.stream.Collectors.toSet());
    Set<String> seriesIds = new HashSet<>();
    parsedTransactions.forEach(transaction -> seriesIds.add(transaction.seriesId()));
    Map<String, List<Transaction>> existingTransactionsBySeries =
        transactionRepository.findAllByEstateIdAndBankImportSeriesIdIn(estateId, seriesIds).stream()
            .collect(java.util.stream.Collectors.groupingBy(Transaction::getBankImportSeriesId));
    List<Transaction> estateTransactions =
        transactionRepository.findAllByEstateIdOrderByDateFromDesc(estateId);
    List<BankStatementTransaction> transactions =
        parsedTransactions.stream()
            .map(
                transaction -> {
                  Transaction existingTransaction =
                      findExistingRecurringTransaction(
                          transaction, existingTransactionsBySeries.get(transaction.seriesId()));
                  boolean reliableRecurringMatch = existingTransaction != null;
                  String category = transaction.category();
                  if (existingTransaction == null && !"OTHER".equals(category)) {
                    existingTransaction =
                        findExistingRecurringTransactionByCategory(
                            transaction, category, estateTransactions);
                    reliableRecurringMatch = existingTransaction != null;
                  }
                  if (existingTransaction == null) {
                    existingTransaction =
                        findExistingRecurringTransactionByAmountOrDirection(
                            transaction, estateTransactions);
                    reliableRecurringMatch =
                        existingTransaction != null
                            && hasSameTransactionName(transaction, existingTransaction);
                  }
                  if ("OTHER".equals(category) && existingTransaction != null) {
                    category = classifyTransaction(existingTransaction.getName());
                  }
                  List<ExistingRecurringTransaction> recurringCandidates =
                      findActiveRecurringTransactionsByDirection(transaction, estateTransactions)
                          .stream()
                          .map(this::toExistingRecurringTransaction)
                          .toList();
                  boolean alreadyRepresented =
                      reliableRecurringMatch
                          && isSameRecurringTransaction(transaction, existingTransaction, category);
                  return transaction
                      .withAlreadyImported(
                          alreadyImportedIds.contains(transaction.importId()) || alreadyRepresented)
                      .withMatch(
                          category,
                          toExistingRecurringTransaction(existingTransaction),
                          recurringCandidates);
                })
            .toList();

    return new BankStatementImportResult(fileName, format, transactions.size(), transactions);
  }

  public List<Map<String, Object>> extractTransactions(Object transactions) {
    return extractItems(transactions, "transactions");
  }

  public List<Map<String, Object>> extractItems(Object itemsObject, String fieldName) {
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

  private List<BankStatementTransaction> parseCsv(byte[] bytes) {
    String content = decodeCsv(bytes).replace("\uFEFF", "");
    List<List<String>> records = parseDelimitedRecords(content, ';');
    int headerIndex = findCsvHeader(records);
    if (headerIndex < 0) {
      throw new SoftiqException("The CSV does not contain the expected Datum and Objem columns");
    }

    List<String> header = records.get(headerIndex);
    Map<String, Integer> columns = new LinkedHashMap<>();
    for (int index = 0; index < header.size(); index++) {
      columns.putIfAbsent(normalizeHeader(header.get(index)), index);
    }

    String accountNumber = findCsvAccountNumber(content);
    List<BankStatementTransaction> result = new ArrayList<>();
    for (int rowIndex = headerIndex + 1; rowIndex < records.size(); rowIndex++) {
      List<String> row = records.get(rowIndex);
      String rawDate = value(row, columns.get("datum"));
      String rawAmount = value(row, columns.get("objem"));
      if (rawDate.isBlank() && rawAmount.isBlank()) {
        continue;
      }

      LocalDate date;
      BigDecimal signedAmount;
      try {
        date = LocalDate.parse(rawDate.trim(), CSV_DATE_FORMAT);
        signedAmount = parseDecimal(rawAmount);
      } catch (DateTimeParseException | NumberFormatException exception) {
        throw new SoftiqException("Invalid CSV transaction on row " + (rowIndex + 1));
      }

      String externalId =
          firstNotBlank(
              value(row, columns.get("id operace")),
              value(row, columns.get("id pokynu")),
              String.valueOf(rowIndex - headerIndex));
      String counterpartyName = value(row, columns.get("nazev protiuctu"));
      String counterpartyAccount = value(row, columns.get("protiucet"));
      String bankCode = value(row, columns.get("kod banky"));
      if (!counterpartyAccount.isBlank() && !bankCode.isBlank()) {
        counterpartyAccount += "/" + bankCode;
      }
      String message = value(row, columns.get("zprava pro prijemce"));
      String note = value(row, columns.get("poznamka"));
      String detail = value(row, columns.get("upresneni"));
      String bankTransactionType = value(row, columns.get("typ"));
      String suggestedName =
          firstNotBlank(
              message, detail, note, counterpartyName, bankTransactionType, "Bankovní transakce");
      String variableSymbol = value(row, columns.get("vs"));
      String currency = firstNotBlank(value(row, columns.get("mena")), "CZK");
      String transactionType = signedAmount.signum() >= 0 ? "INCOME" : "EXPENSE";
      BigDecimal amount = signedAmount.abs();
      String importId = createImportId(accountNumber, externalId, date, signedAmount);
      String seriesId =
          createSeriesId(
              accountNumber,
              transactionType,
              currency,
              counterpartyAccount,
              variableSymbol,
              counterpartyName,
              suggestedName);

      result.add(
          new BankStatementTransaction(
              importId,
              seriesId,
              stripLeadingZeros(externalId),
              date.toString(),
              amount,
              currency,
              transactionType,
              suggestedName,
              counterpartyName,
              counterpartyAccount,
              variableSymbol,
              message,
              bankTransactionType,
              false,
              classifyTransaction(suggestedName, counterpartyName, message),
              null,
              List.of()));
    }
    return result;
  }

  private List<BankStatementTransaction> parseGpc(byte[] bytes) {
    String content = new String(bytes, WINDOWS_1250);
    String accountNumber = "";
    List<BankStatementTransaction> result = new ArrayList<>();
    int lineNumber = 0;
    for (String rawLine : content.split("\\R")) {
      lineNumber++;
      String line = rawLine.stripTrailing();
      if (line.startsWith("074") && line.length() >= 19) {
        accountNumber = stripLeadingZeros(line.substring(3, 19));
        continue;
      }
      if (!line.startsWith("075")) {
        continue;
      }
      if (line.length() < 128) {
        throw new SoftiqException("Invalid GPC transaction on row " + lineNumber);
      }

      String externalId = stripLeadingZeros(line.substring(35, 48));
      BigDecimal amount;
      try {
        amount = new BigDecimal(line.substring(48, 60)).movePointLeft(2);
      } catch (NumberFormatException exception) {
        throw new SoftiqException("Invalid GPC amount on row " + lineNumber);
      }
      char accountingCode = line.charAt(60);
      String transactionType =
          switch (accountingCode) {
            case '1', '5' -> "EXPENSE";
            case '2', '4' -> "INCOME";
            default -> throw new SoftiqException(
                "Unknown GPC accounting code on row " + lineNumber);
          };
      BigDecimal signedAmount = "EXPENSE".equals(transactionType) ? amount.negate() : amount;
      LocalDate date =
          parseGpcDate(firstNotBlank(line.substring(122, 128), line.substring(91, 97)), lineNumber);
      String counterpartyName = line.substring(97, 117).trim();
      String counterpartyAccount = stripLeadingZeros(line.substring(19, 35));
      String bankAndConstantSymbol = line.substring(71, 81);
      String bankCode = bankAndConstantSymbol.substring(2, 6);
      if (!counterpartyAccount.isBlank() && !bankCode.equals("0000")) {
        counterpartyAccount += "/" + bankCode;
      }
      String variableSymbol = stripLeadingZeros(line.substring(61, 71));
      String currencyCode = line.substring(118, 122);
      String currency = "0203".equals(currencyCode) ? "CZK" : currencyCode;
      String suggestedName = firstNotBlank(counterpartyName, "Bankovní transakce");
      String importId = createImportId(accountNumber, externalId, date, signedAmount);
      String seriesId =
          createSeriesId(
              accountNumber,
              transactionType,
              currency,
              counterpartyAccount,
              variableSymbol,
              counterpartyName,
              suggestedName);

      result.add(
          new BankStatementTransaction(
              importId,
              seriesId,
              externalId,
              date.toString(),
              amount,
              currency,
              transactionType,
              suggestedName,
              counterpartyName,
              counterpartyAccount,
              variableSymbol,
              "",
              "GPC",
              false,
              classifyTransaction(suggestedName, counterpartyName),
              null,
              List.of()));
    }
    return result;
  }

  private String decodeCsv(byte[] bytes) {
    try {
      return StandardCharsets.UTF_8
          .newDecoder()
          .onMalformedInput(CodingErrorAction.REPORT)
          .onUnmappableCharacter(CodingErrorAction.REPORT)
          .decode(ByteBuffer.wrap(bytes))
          .toString();
    } catch (CharacterCodingException exception) {
      return new String(bytes, WINDOWS_1250);
    }
  }

  private List<List<String>> parseDelimitedRecords(String content, char delimiter) {
    List<List<String>> records = new ArrayList<>();
    List<String> record = new ArrayList<>();
    StringBuilder value = new StringBuilder();
    boolean quoted = false;
    for (int index = 0; index < content.length(); index++) {
      char character = content.charAt(index);
      if (character == '"') {
        if (quoted && index + 1 < content.length() && content.charAt(index + 1) == '"') {
          value.append('"');
          index++;
        } else {
          quoted = !quoted;
        }
      } else if (character == delimiter && !quoted) {
        record.add(value.toString());
        value.setLength(0);
      } else if ((character == '\n' || character == '\r') && !quoted) {
        if (character == '\r'
            && index + 1 < content.length()
            && content.charAt(index + 1) == '\n') {
          index++;
        }
        record.add(value.toString());
        value.setLength(0);
        records.add(record);
        record = new ArrayList<>();
      } else {
        value.append(character);
      }
    }
    if (!record.isEmpty() || !value.isEmpty()) {
      record.add(value.toString());
      records.add(record);
    }
    return records;
  }

  private int findCsvHeader(List<List<String>> records) {
    for (int recordIndex = 0; recordIndex < records.size(); recordIndex++) {
      Set<String> values = new HashSet<>();
      records.get(recordIndex).forEach(value -> values.add(normalizeHeader(value)));
      if (values.contains("datum") && values.contains("objem")) {
        return recordIndex;
      }
    }
    return -1;
  }

  private String findCsvAccountNumber(String content) {
    Matcher matcher = ACCOUNT_NUMBER_PATTERN.matcher(content);
    String accountNumber = "";
    while (matcher.find()) {
      String candidate = matcher.group(1).split("/")[0];
      if (candidate.replace("-", "").length() > accountNumber.replace("-", "").length()) {
        accountNumber = candidate;
      }
    }
    return accountNumber;
  }

  private String normalizeHeader(String value) {
    String normalized = Normalizer.normalize(value, Normalizer.Form.NFD);
    return normalized
        .replaceAll("\\p{M}", "")
        .replace("\uFEFF", "")
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private String value(List<String> row, Integer index) {
    return index != null && index >= 0 && index < row.size() ? row.get(index).trim() : "";
  }

  private BigDecimal parseDecimal(String value) {
    return new BigDecimal(value.replace("\u00A0", "").replace(" ", "").replace(',', '.'));
  }

  private LocalDate parseGpcDate(String rawDate, int lineNumber) {
    try {
      int day = Integer.parseInt(rawDate.substring(0, 2));
      int month = Integer.parseInt(rawDate.substring(2, 4));
      int year = 2000 + Integer.parseInt(rawDate.substring(4, 6));
      return LocalDate.of(year, month, day);
    } catch (DateTimeException | NumberFormatException | IndexOutOfBoundsException exception) {
      throw new SoftiqException("Invalid GPC date on row " + lineNumber);
    }
  }

  private String createImportId(
      String accountNumber, String externalId, LocalDate date, BigDecimal signedAmount) {
    String value =
        stripLeadingZeros(accountNumber)
            + "|"
            + stripLeadingZeros(externalId)
            + "|"
            + date
            + "|"
            + signedAmount.stripTrailingZeros().toPlainString();
    return createHash(value);
  }

  private Transaction findExistingRecurringTransaction(
      BankStatementTransaction incomingTransaction, List<Transaction> candidates) {
    if (candidates == null || candidates.isEmpty()) {
      return null;
    }

    LocalDate incomingDate = LocalDate.parse(incomingTransaction.date());
    return candidates.stream()
        .filter(candidate -> candidate.getRecurrence() != null)
        .filter(candidate -> candidate.getRecurrence() != Recurrence.ONCE)
        .filter(candidate -> candidate.getDateFrom() != null)
        .filter(candidate -> toLocalDate(candidate.getDateFrom()).isBefore(incomingDate))
        .filter(
            candidate ->
                candidate.getDateTo() == null
                    || !toLocalDate(candidate.getDateTo()).isBefore(incomingDate))
        .max(java.util.Comparator.comparing(Transaction::getDateFrom))
        .orElse(null);
  }

  private Transaction findExistingRecurringTransactionByCategory(
      BankStatementTransaction incomingTransaction, String category, List<Transaction> candidates) {
    List<Transaction> categoryCandidates =
        candidates.stream()
            .filter(candidate -> candidate.getTransactionType() != null)
            .filter(
                candidate ->
                    candidate
                        .getTransactionType()
                        .name()
                        .equals(incomingTransaction.transactionType()))
            .filter(candidate -> category.equals(classifyTransaction(candidate.getName())))
            .toList();
    return findExistingRecurringTransaction(incomingTransaction, categoryCandidates);
  }

  private Transaction findExistingRecurringTransactionByAmountOrDirection(
      BankStatementTransaction incomingTransaction, List<Transaction> candidates) {
    List<Transaction> sameDirectionCandidates =
        candidates.stream()
            .filter(candidate -> candidate.getTransactionType() != null)
            .filter(
                candidate ->
                    candidate
                        .getTransactionType()
                        .name()
                        .equals(incomingTransaction.transactionType()))
            .toList();
    List<Transaction> sameAmountCandidates =
        sameDirectionCandidates.stream()
            .filter(candidate -> candidate.getAmount() != null)
            .filter(candidate -> candidate.getAmount().compareTo(incomingTransaction.amount()) == 0)
            .toList();
    Transaction amountMatch =
        findExistingRecurringTransaction(incomingTransaction, sameAmountCandidates);
    if (amountMatch != null) {
      return amountMatch;
    }

    List<Transaction> activeCandidates =
        sameDirectionCandidates.stream()
            .filter(
                candidate ->
                    findExistingRecurringTransaction(incomingTransaction, List.of(candidate))
                        != null)
            .toList();
    return activeCandidates.size() == 1 ? activeCandidates.getFirst() : null;
  }

  private List<Transaction> findActiveRecurringTransactionsByDirection(
      BankStatementTransaction incomingTransaction, List<Transaction> candidates) {
    return candidates.stream()
        .filter(candidate -> candidate.getTransactionType() != null)
        .filter(
            candidate ->
                candidate.getTransactionType().name().equals(incomingTransaction.transactionType()))
        .filter(
            candidate ->
                findExistingRecurringTransaction(incomingTransaction, List.of(candidate)) != null)
        .toList();
  }

  private boolean isSameRecurringTransaction(
      BankStatementTransaction incomingTransaction,
      Transaction existingTransaction,
      String category) {
    if (existingTransaction == null
        || existingTransaction.getAmount() == null
        || existingTransaction.getTransactionType() == null
        || existingTransaction.getRecurrence() == null) {
      return false;
    }

    Recurrence expectedRecurrence =
        "PROPERTY_TAX".equals(category) || "INSURANCE".equals(category)
            ? Recurrence.YEARLY
            : Recurrence.MONTHLY;
    return existingTransaction.getAmount().compareTo(incomingTransaction.amount()) == 0
        && existingTransaction
            .getTransactionType()
            .name()
            .equals(incomingTransaction.transactionType())
        && existingTransaction.getRecurrence() == expectedRecurrence;
  }

  private boolean hasSameTransactionName(
      BankStatementTransaction incomingTransaction, Transaction existingTransaction) {
    String incomingName = normalizeTransactionName(incomingTransaction.suggestedName());
    String existingName = normalizeTransactionName(existingTransaction.getName());
    return !incomingName.isBlank() && incomingName.equals(existingName);
  }

  private String normalizeTransactionName(String value) {
    return normalizeText(value).replaceAll("[^a-z0-9]+", " ").trim().replaceAll("\\s+", " ");
  }

  private ExistingRecurringTransaction toExistingRecurringTransaction(Transaction transaction) {
    if (transaction == null) {
      return null;
    }
    return new ExistingRecurringTransaction(
        transaction.getId(),
        transaction.getName(),
        transaction.getAmount(),
        transaction.getRecurrence().name(),
        toLocalDate(transaction.getDateFrom()).toString(),
        transaction.getDateTo() == null ? null : toLocalDate(transaction.getDateTo()).toString());
  }

  private LocalDate toLocalDate(java.util.Date date) {
    return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
  }

  private String classifyTransaction(String... values) {
    String normalized = normalizeText(String.join(" ", values));
    if (containsAny(normalized, "najem", "najemne", "rent")) return "RENT";
    if (containsAny(normalized, "voda", "vodne", "stocne", "water")) return "WATER";
    if (containsAny(normalized, "elektrina", "energie", "electricity", "power")) {
      return "ELECTRICITY";
    }
    if (containsAny(normalized, "internet", "wifi")) return "INTERNET";
    if (containsAny(normalized, "zaloha", "zalohy", "advance payment")) {
      return "ADVANCE_PAYMENTS";
    }
    if (containsAny(normalized, "pojisteni", "pojistne", "insurance")) return "INSURANCE";
    if (containsAny(normalized, "dan z bytu", "dan z nemovitosti", "property tax")) {
      return "PROPERTY_TAX";
    }
    return "OTHER";
  }

  private String normalizeText(String value) {
    return Normalizer.normalize(value == null ? "" : value, Normalizer.Form.NFD)
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT);
  }

  private boolean containsAny(String value, String... candidates) {
    for (String candidate : candidates) {
      if (value.contains(candidate)) return true;
    }
    return false;
  }

  private String createSeriesId(
      String accountNumber,
      String transactionType,
      String currency,
      String counterpartyAccount,
      String variableSymbol,
      String counterpartyName,
      String suggestedName) {
    String normalizedAccount = stripLeadingZeros(counterpartyAccount).toLowerCase(Locale.ROOT);
    String normalizedVariableSymbol = stripLeadingZeros(variableSymbol);
    String fallbackIdentity =
        normalizedAccount.isBlank() && normalizedVariableSymbol.isBlank()
            ? firstNotBlank(counterpartyName, suggestedName).toLowerCase(Locale.ROOT)
            : "";
    String value =
        stripLeadingZeros(accountNumber).toLowerCase(Locale.ROOT)
            + "|"
            + transactionType
            + "|"
            + currency.toUpperCase(Locale.ROOT)
            + "|"
            + normalizedAccount
            + "|"
            + normalizedVariableSymbol
            + "|"
            + fallbackIdentity;
    return createHash(value);
  }

  private String createHash(String value) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 is not available", exception);
    }
  }

  private String stripLeadingZeros(String value) {
    if (value == null) {
      return "";
    }
    String stripped = value.trim().replaceFirst("^0+(?!$)", "");
    return "0".equals(stripped) ? "" : stripped;
  }

  private String firstNotBlank(String... values) {
    for (String value : values) {
      if (value != null && !value.isBlank()) {
        return value.trim();
      }
    }
    return "";
  }
}
