package cz.softiq.service.realiq.service.email;

import cz.softiq.core.email.ICoreEmailService;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.PortfolioRecordEntity;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.records.TransactionReminderEmail;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
class RealiqEmailComponent implements ICoreEmailService, ICoreSoftiqService {
  private final TemplateEngine templateEngine;

  @Value("${spring.mail.username}")
  private String adminEmail;

  public TransactionReminderEmail transactionReminder(
      ClientEstate estate,
      Transaction transaction,
      String period,
      String ownerEmail,
      String ownerName) {
    YearMonth reminderPeriod =
        YearMonth.parse(
            period, DateTimeFormatter.ofPattern("MM uuuu").withResolverStyle(ResolverStyle.STRICT));
    Locale locale = Locale.forLanguageTag("cs-CZ");
    String periodLabel = reminderPeriod.format(DateTimeFormatter.ofPattern("LLLL uuuu", locale));

    Context context = new Context(locale);
    context.setVariable("period", periodLabel);
    context.setVariable("estateName", estate.getName());
    context.setVariable("transactionName", transaction.getName());
    context.setVariable("ownerEmail", ownerEmail);
    context.setVariable("ownerName", ownerName);

    String content = templateEngine.process("email/transaction-reminder.html", context);
    String subject = "Připomínka platby – " + periodLabel;
    String senderName = getReminderSenderName(ownerName);
    sendEmail(
        subject, content, estate.getTenantEmail().trim(), adminEmail, senderName, ownerEmail, null);
    return new TransactionReminderEmail(subject, content);
  }

  private String getReminderSenderName(String ownerName) {
    if (ownerName == null || ownerName.isBlank()) {
      return "SOFTIQ";
    }
    return ownerName.trim() + " via SOFTIQ";
  }

  public void portfolioRecordReminder(PortfolioRecordEntity record, LocalDate occurrenceDate) {
    if (record.getClient() == null
        || record.getClient().getEmail() == null
        || record.getClient().getEmail().isBlank()) {
      return;
    }

    Locale locale = Locale.forLanguageTag("cs-CZ");
    Context context = new Context(locale);
    context.setVariable("recordTitle", record.getTitle());
    context.setVariable("estateName", record.getEstate().getName());
    context.setVariable(
        "occurrenceDate", occurrenceDate.format(DateTimeFormatter.ofPattern("d. M. uuuu", locale)));
    context.setVariable("note", record.getNote());
    context.setVariable("documentCount", record.getDocuments().size());

    String content = templateEngine.process("email/portfolio-record-reminder.html", context);
    sendEmail(
        "Připomínka záznamu – " + record.getTitle(),
        content,
        record.getClient().getEmail().trim(),
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }
}
