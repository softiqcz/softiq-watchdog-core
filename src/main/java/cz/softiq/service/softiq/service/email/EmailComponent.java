package cz.softiq.service.softiq.service.email;

import static cz.softiq.service.softiq.model.enums.ContextType.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import cz.softiq.common.base.model.IBaseComponent;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.email.ICoreEmailService;
import cz.softiq.core.model.TestReport;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.watchdogSearch.service.WatchDogOfferUtils;
import cz.softiq.service.automation.watchdogSearch.service.WatchdogResultAnalysisComponent;
import cz.softiq.service.realiq.model.PortfolioMonthlySummary;
import cz.softiq.service.softiq.entity.PromoEmailEntity;
import cz.softiq.service.softiq.model.enums.ContextType;
import cz.softiq.service.softiq.model.enums.PromoEmailType;
import cz.softiq.service.softiq.repository.PromoEmailRepository;
import cz.softiq.service.softiq.repository.UnsubscribeRepository;
import cz.softiq.service.softiq.service.unsubscribe.IUnsubscribeService;
import cz.softiq.service.watchdog.entity.InvoiceEntity;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.WatchdogMonthlySummary;
import cz.softiq.service.watchdog.model.WatchdogResultAnalysisEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.service.invoice.IInvoiceService;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailComponent
    implements IBaseComponent,
        IInvoiceService,
        ICoreEmailService,
        ICoreSoftiqService,
        IUnsubscribeService {
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final TemplateEngine templateEngine;
  private final PromoEmailRepository promoEmailRepository;
  private final UnsubscribeRepository unsubscribeRepository;
  private final WatchdogResultAnalysisComponent watchdogResultAnalysisComponent;

  @Value("${spring.mail.username}")
  private String adminEmail;

  public String renderWatchDogReport(Context context) {
    return templateEngine.process("email/watchdog-report-new.html", context);
  }

  public void sendPortfolioNewsEmail(String emailTo) {
    if (!canSendEmail(emailTo)) {
      return;
    }

    Context context = getContext(PORTFOLIO_NEWS, emailTo);
    String content = templateEngine.process("email/portfolio-news.html", context);
    sendEmail(
        "Novinka v SOFTIQ: představujeme Portfolio nemovitostí",
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public boolean sendNoActivityPromoEmail(
      CoreClientEntity client, LocalDateTime monthlyCutoff, LocalDateTime sentAt) {
    return sendPromoEmailIfDue(
        client,
        PromoEmailType.NO_ACTIVITY,
        NO_ACTIVITY,
        monthlyCutoff,
        sentAt,
        "Softiq čeká — zbývá vám 9 dní zdarma",
        "email/no-activity.html");
  }

  public boolean sendCreateWatchdogPromoEmail(
      CoreClientEntity client, LocalDateTime monthlyCutoff, LocalDateTime sentAt) {
    return sendPromoEmailIfDue(
        client,
        PromoEmailType.CREATE_WATCHDOG,
        PROMO_CREATE_WATCHDOG,
        monthlyCutoff,
        sentAt,
        "Realitní hlídací pes: dřív, než nabídka zmizí",
        "email/promo-create-watchdog.html");
  }

  public boolean sendCreatePortfolioPromoEmail(
      CoreClientEntity client, LocalDateTime monthlyCutoff, LocalDateTime sentAt) {
    return sendPromoEmailIfDue(
        client,
        PromoEmailType.CREATE_PORTFOLIO,
        PROMO_CREATE_PORTFOLIO,
        monthlyCutoff,
        sentAt,
        "Mějte jasno v tom, co vlastníte. I co to může přinést.",
        "email/promo-create-portfolio.html");
  }

  public boolean sendAddTransactionsPromoEmail(
      CoreClientEntity client, LocalDateTime monthlyCutoff, LocalDateTime sentAt) {
    return sendPromoEmailIfDue(
        client,
        PromoEmailType.ADD_PORTFOLIO_TRANSACTIONS,
        PROMO_ADD_TRANSACTIONS,
        monthlyCutoff,
        sentAt,
        "Doplňte transakce a získejte skutečný finanční přehled",
        "email/promo-add-transactions.html");
  }

  private boolean sendPromoEmailIfDue(
      CoreClientEntity client,
      PromoEmailType emailType,
      ContextType contextType,
      LocalDateTime monthlyCutoff,
      LocalDateTime sentAt,
      String subject,
      String template) {
    try {
      if (promoEmailRepository.existsByClientAndEmailTypeAndSentAtGreaterThanEqual(
          client, emailType, monthlyCutoff)) {
        return false;
      }

      Context context = getContext(contextType, client.getEmail());
      String content = templateEngine.process(template, context);

      if (unsubscribeRepository.existsByClientEmail(client.getEmail())) {
        log.info("Skipping promo email for unsubscribed client {}", client.getId());
        return false;
      }

      sendEmail(subject, content, client.getEmail(), adminEmail, "SOFTIQ", null, null);
      promoEmailRepository.save(new PromoEmailEntity(null, client, emailType, sentAt));
      log.info("Sent promo email {} to client {}", emailType, client.getId());
      return true;
    } catch (RuntimeException exception) {
      log.error("Could not send promo email {} to client {}", emailType, client.getId(), exception);
      saveLog(
          new CoreLogEntity(
              null,
              LocalDate.now(),
              LocalDateTime.now(),
              "Could not send promo email %s to client %s: %s"
                  .formatted(emailType, client.getId(), exception.getMessage())));
      return false;
    }
  }

  private File generatePdfInvoice(WatchDogMembershipEntity client) {
    LocalDate today = LocalDate.now();
    String issueDate = today.format(DateTimeFormatter.ofPattern("d.M.yyyy"));
    Integer invoiceNumber = getNewInvoiceNumber();

    Context context = new Context();
    Map<String, Object> variables = new HashMap<>();
    variables.put("invoiceNumber", invoiceNumber);
    variables.put("issueDate", issueDate);
    variables.put("buyerName", client != null ? client.getSubject() : null);
    variables.put("buyerAddress", client != null ? client.getAddress() : null);
    variables.put("buyerIC", client != null ? client.getIdNumber() : null);
    variables.put(
        "buyerEmail",
        client != null && client.getClient() != null ? client.getClient().getEmail() : null);

    context.setVariables(variables);

    Map<String, Object> product =
        Map.of(
            "code", client.getMembership().getName(),
            "quantity", "1",
            "description", client.getMembership().getMaxCountOfWatchdogsText(),
            "total", client.getAgreedPrice());
    context.setVariable("product", product);

    String html = templateEngine.process("email/pdf-invoice.html", context);

    try {
      File invoiceFile = File.createTempFile("invoice-", ".pdf");
      try (OutputStream os = new FileOutputStream(invoiceFile)) {
        PdfRendererBuilder builder = new PdfRendererBuilder();
        builder.useFastMode();
        builder.withHtmlContent(html, null);
        builder.toStream(os);
        builder.run();
      }
      byte[] fileBytes = Files.readAllBytes(invoiceFile.toPath());

      InvoiceEntity invoiceEntity =
          new InvoiceEntity(
              today,
              client.getSubject(),
              client.getAddress(),
              client.getIdNumber(),
              invoiceNumber,
              client.getAgreedPrice(),
              client.getMembership().getMaxCountOfWatchdogsText(),
              fileBytes);
      saveInvoice(invoiceEntity);
      return invoiceFile;
    } catch (Exception e) {
      saveLog(
          new CoreLogEntity(
              null,
              LocalDate.now(),
              LocalDateTime.now(),
              "Error creating invoice " + e.getMessage()));
      throw new SoftiqException("Error creating invoice");
    }
  }

  @SuppressWarnings("unchecked")
  public Context getContext(ContextType type, Object... args) {

    return switch (type) {
      case NO_ACTIVITY,
          MISS_YOU,
          PORTFOLIO_NEWS,
          PROMO_CREATE_WATCHDOG,
          PROMO_CREATE_PORTFOLIO,
          PROMO_ADD_TRANSACTIONS -> {
        checkArgCount((ContextType) type, args, 1, "String");
        yield buildUnsubscribeContext((String) args[0]);
      }

      case GROUPED_OFFERS -> {
        checkArgCount((ContextType) type, args, 1, "List<List<WatchDogEstatesEntity>>");
        yield buildGroupedOffersContext((List<List<WatchDogEstatesEntity>>) args[0]);
      }

      case TEST_REPORT -> {
        checkArgCount((ContextType) type, args, 1, "TestReport testReport");
        yield buildTestReportContext((TestReport) args[0]);
      }

      case OFFER -> {
        checkArgCount((ContextType) type, args, 1, "WatchDogEstatesEntity");
        yield buildOfferContext((WatchDogEstatesEntity) args[0]);
      }

      case PASSWORD_RESET -> {
        checkArgCount((ContextType) type, args, 1, "String (url)");
        yield buildUrlContext((String) args[0]);
      }

      case MEMBERSHIP_CHANGE -> {
        checkArgCount(
            (ContextType) type,
            args,
            4,
            "MembershipEnum, String price, String oldPrice, WatchDogMembershipEntity");
        yield buildMembershipContext(
            (MembershipEnum) args[0],
            (String) args[1],
            (String) args[2],
            (WatchDogMembershipEntity) args[3]);
      }

      case MEMBERSHIP_PROLONG -> {
        checkArgCount(
            (ContextType) type, args, 3, "MembershipEnum, String price, WatchDogMembershipEntity");
        yield buildMembershipContext(
            (MembershipEnum) args[0], (String) args[1], (WatchDogMembershipEntity) args[2]);
      }

      case EXPIRY_REMINDER -> {
        checkArgCount(
            (ContextType) type,
            args,
            4,
            "WatchDogMembershipEntity, List<WatchDogEstatesEntity>, Integer daysLeft, String title");
        yield buildExpiryContext(
            (WatchDogMembershipEntity) args[0],
            (List<WatchDogEstatesEntity>) args[1],
            (Integer) args[2],
            (String) args[3]);
      }

      case PAYMENT_FAILED -> {
        checkArgCount((ContextType) type, args, 2, "WatchDogMembershipEntity, String title");
        yield buildPaymentFailedContext((WatchDogMembershipEntity) args[0], (String) args[1]);
      }

      case EXPIRY_TODAY -> {
        checkArgCount(
            (ContextType) type, args, 2, "WatchDogMembershipEntity, List<WatchDogEstatesEntity>");
        yield buildExpiryContext(
            (WatchDogMembershipEntity) args[0],
            (List<WatchDogEstatesEntity>) args[1],
            0,
            "Vaše předplatné dnes končí");
      }

      case WATCHDOG_MONTHLY_SUMMARY -> {
        checkArgCount(
            (ContextType) type,
            args,
            3,
            "WatchDogEntity, WatchdogMonthlySummary, String (emailTo)");
        yield buildMonthlySummaryContext(
            (WatchDogEntity) args[0], (WatchdogMonthlySummary) args[1], (String) args[2]);
      }

      case PORTFOLIO_MONTHLY_SUMMARY -> {
        checkArgCount((ContextType) type, args, 2, "PortfolioMonthlySummary, String (emailTo)");
        yield buildPortfolioMonthlySummaryContext(
            (PortfolioMonthlySummary) args[0], (String) args[1]);
      }
    };
  }

  private void checkArgCount(ContextType type, Object[] args, int expected, String expectedDesc) {
    if (args == null || args.length != expected) {
      throw new IllegalArgumentException(
          "Invalid args for " + type + ". Expected: " + expectedDesc);
    }
  }

  /* --- private builders --- */

  private Context buildUnsubscribeContext(String emailTo) {
    Context ctx = new Context();
    ctx.setVariable("unsubscribeUrl", generateUnsubscribeLink(emailTo));
    return ctx;
  }

  private Context buildGroupedOffersContext(List<List<WatchDogEstatesEntity>> groupedOffers) {
    Context ctx = new Context();
    var listWatchDogs = new ArrayList<WatchDogEntity>();
    var listWatchdogEstates = new ArrayList<WatchDogEstatesEntity>();
    if (groupedOffers != null) {
      for (List<WatchDogEstatesEntity> group : groupedOffers) {
        if (group != null && !group.isEmpty()) {
          listWatchDogs.add(group.getFirst().getWatchdogEntity());
          listWatchdogEstates.addAll(group);
        }
      }
    }
    ctx.setVariable("listWatchDogs", listWatchDogs);
    ctx.setVariable("listWatchdogEstates", listWatchdogEstates);
    ctx.setVariable(
        "watchdogResultAnalyses", buildWatchdogResultAnalyses(listWatchDogs, listWatchdogEstates));
    return ctx;
  }

  private Map<Integer, WatchdogResultAnalysisEntity> buildWatchdogResultAnalyses(
      List<WatchDogEntity> watchDogs, List<WatchDogEstatesEntity> currentOffers) {
    currentOffers.forEach(WatchDogOfferUtils::populateMissingAreaAndPricePerSquareMeter);
    Map<Integer, WatchdogResultAnalysisEntity> analyses = new HashMap<>();
    for (WatchDogEntity watchDog : watchDogs) {
      List<WatchDogEstatesEntity> offersForWatchDog =
          currentOffers.stream()
              .filter(offer -> offer.getWatchdogEntity().getId().equals(watchDog.getId()))
              .toList();
      analyses.put(
          watchDog.getId(), watchdogResultAnalysisComponent.analyze(watchDog, offersForWatchDog));
    }
    return analyses;
  }

  private Context buildTestReportContext(TestReport testReport) {
    Context ctx = new Context();
    String result = testReport.getResult() != null ? testReport.getResult().name() : "INFO";
    Integer successCount = testReport.getSuccessCount() != null ? testReport.getSuccessCount() : 0;
    Integer errorCount = testReport.getErrorCount() != null ? testReport.getErrorCount() : 0;
    Integer totalCount =
        testReport.getTotalCount() != null ? testReport.getTotalCount() : successCount + errorCount;
    List<String> successMessages =
        testReport.getSuccessMessages() != null ? testReport.getSuccessMessages() : List.of();
    List<String> errorMessages =
        testReport.getErrorMessages() != null ? testReport.getErrorMessages() : List.of();

    ctx.setVariable("brandSub", "Test report");
    ctx.setVariable("badgeText", result);
    ctx.setVariable("sectionTitle", "Průběh testovací sady");
    ctx.setVariable("reportTitle", defaultText(testReport.getName(), "Test suite"));
    ctx.setVariable(
        "reportMeta",
        String.format(
            "Profil %s • %s",
            defaultText(testReport.getProfile(), "-"),
            formatDateTime(
                testReport.getFinishedAt() != null
                    ? testReport.getFinishedAt()
                    : LocalDateTime.now())));
    ctx.setVariable("reportFooterNote", "Automaticky odeslaný report po dokončení testovací sady.");
    ctx.setVariable(
        "summaryText",
        errorCount > 0
            ? "Běh testovací sady skončil s chybami. Níže najdete chybové výpisy a souhrn běhu."
            : "Běh testovací sady proběhl úspěšně. Níže je souhrn běhu a záznamy o provedených krocích.");
    ctx.setVariable("resultText", result);
    ctx.setVariable("profile", defaultText(testReport.getProfile(), "-"));
    ctx.setVariable("startedAt", formatDateTime(testReport.getStartedAt()));
    ctx.setVariable("finishedAt", formatDateTime(testReport.getFinishedAt()));
    ctx.setVariable("duration", defaultText(testReport.getDuration(), "-"));
    ctx.setVariable("successCount", successCount);
    ctx.setVariable("errorCount", errorCount);
    ctx.setVariable("totalCount", totalCount);
    ctx.setVariable("successMessages", successMessages);
    ctx.setVariable("errorMessages", errorMessages);
    return ctx;
  }

  private String formatDateTime(LocalDateTime dateTime) {
    return dateTime != null
        ? dateTime.format(DateTimeFormatter.ofPattern("d.M.yyyy H:mm:ss"))
        : "-";
  }

  private String defaultText(String value, String defaultValue) {
    return value != null && !value.isBlank() ? value : defaultValue;
  }

  private Context buildOfferContext(WatchDogEstatesEntity offer) {
    Context ctx = new Context();
    var listWatchDogs = new ArrayList<WatchDogEntity>();
    var listWatchdogEstates = new ArrayList<WatchDogEstatesEntity>();

    if (offer != null) {
      String subject =
          (offer.getIsUpdated() ? "Aktualizováno: " : "")
              + offer.getWatchdogEntity().getExactAddress()
              + " "
              + offer.getHeader();

      listWatchDogs.add(offer.getWatchdogEntity());
      listWatchdogEstates.add(offer);

      ctx.setVariable("subject", subject);
    }

    ctx.setVariable("listWatchDogs", listWatchDogs);
    ctx.setVariable("listWatchdogEstates", listWatchdogEstates);
    ctx.setVariable(
        "watchdogResultAnalyses", buildWatchdogResultAnalyses(listWatchDogs, listWatchdogEstates));
    return ctx;
  }

  private Context buildUrlContext(String url) {
    Context ctx = new Context();
    ctx.setVariable("url", url);
    return ctx;
  }

  private Context buildMembershipContext(
      MembershipEnum membership, String price, String oldPrice, WatchDogMembershipEntity client) {
    Context ctx = new Context();
    ctx.setVariable("membership", membership);
    ctx.setVariable("price", price);
    if (oldPrice != null) ctx.setVariable("oldPrice", oldPrice);
    if (client != null) ctx.setVariable("valid_to", client.getValidToFormatted());
    return ctx;
  }

  private Context buildMembershipContext(
      MembershipEnum membership, String price, WatchDogMembershipEntity client) {
    return buildMembershipContext(membership, price, null, client);
  }

  private Context buildExpiryContext(
      WatchDogMembershipEntity client,
      List<WatchDogEstatesEntity> estates,
      int daysLeft,
      String title) {
    Context ctx = new Context();
    int size = estates == null ? 0 : estates.size();
    ctx.setVariable("title", title);
    ctx.setVariable("days", String.valueOf(daysLeft));
    ctx.setVariable("hours", size == 0 ? "19 hodin" : "17,5 hodin");
    ctx.setVariable("price", MembershipEnum.STANDARD.getPrice() + " Kč");

    ctx.setVariable("size", String.valueOf(size));
    if (client != null) {
      var coreClient = client.getClient();
      ctx.setVariable("coreClient", coreClient);
      ctx.setVariable("membership", client.getMembership().getName());
      ctx.setVariable("clientName", coreClient.getClientName());
    }
    return ctx;
  }

  private Context buildPaymentFailedContext(WatchDogMembershipEntity client, String title) {
    Context ctx = new Context();
    ctx.setVariable("title", title);
    ctx.setVariable("clientName", client.getClient().getClientName());
    return ctx;
  }

  private Context buildMonthlySummaryContext(
      WatchDogEntity watchDog, WatchdogMonthlySummary summary, String emailTo) {
    Context ctx = new Context();
    ctx.setVariable("watchdogName", watchDog.getExactAddress());
    ctx.setVariable("watchdog", watchDog);
    ctx.setVariable("summary", summary);
    ctx.setVariable("unsubscribeUrl", generateUnsubscribeLink(emailTo));
    return ctx;
  }

  public void sendWatchdogMonthlySummary(
      WatchDogEntity watchDog, WatchdogMonthlySummary summary, String emailTo) {
    if (!canSendEmail(emailTo)) {
      return;
    }

    String monthName = getMonthName(LocalDate.now().getMonthValue() - 1);
    Context context = getContext(WATCHDOG_MONTHLY_SUMMARY, watchDog, summary, emailTo);
    String content = templateEngine.process("email/watchdog-monthly-summary.html", context);
    sendEmail(
        "Měsíční přehled: " + watchDog.getExactAddress() + " – " + monthName,
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public void sendClientWatchdogsMonthlySummary(
      cz.softiq.service.watchdog.model.ClientWatchdogsSummary clientSummary, String emailTo) {
    if (!canSendEmail(emailTo)) {
      return;
    }

    String monthName = getMonthName(LocalDate.now().getMonthValue() - 1);
    Context context = new Context();
    context.setVariable("clientSummary", clientSummary);
    context.setVariable("unsubscribeUrl", generateUnsubscribeLink(emailTo));

    String content = templateEngine.process("email/watchdog-monthly-summary.html", context);
    sendEmail(
        "Měsíční přehled hlídacího psa – " + monthName,
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  private String getMonthName(int month) {
    return switch (month) {
      case 1 -> "leden";
      case 2 -> "únor";
      case 3 -> "březen";
      case 4 -> "duben";
      case 5 -> "květen";
      case 6 -> "červen";
      case 7 -> "červenec";
      case 8 -> "srpen";
      case 9 -> "září";
      case 10 -> "říjen";
      case 11 -> "listopad";
      case 12 -> "prosinec";
      default -> "měsíc";
    };
  }

  private Context buildPortfolioMonthlySummaryContext(
      PortfolioMonthlySummary summary, String emailTo) {
    Context ctx = new Context();
    ctx.setVariable("summary", summary);
    ctx.setVariable("unsubscribeUrl", generateUnsubscribeLink(emailTo));
    return ctx;
  }

  private Context buildPortfolioEstatesSummaryContext(
      cz.softiq.service.realiq.model.PortfolioEstatesSummary estatesSummary, String emailTo) {
    Context ctx = new Context();
    ctx.setVariable("estatesSummary", estatesSummary);
    ctx.setVariable("unsubscribeUrl", generateUnsubscribeLink(emailTo));
    return ctx;
  }

  public void sendPortfolioEstatesMonthlySummary(
      cz.softiq.service.realiq.model.PortfolioEstatesSummary estatesSummary, String emailTo) {
    if (!canSendEmail(emailTo)) {
      return;
    }

    String monthName =
        getMonthName(LocalDate.now(ZoneId.of("Europe/Prague")).minusMonths(1).getMonthValue());
    Context context = buildPortfolioEstatesSummaryContext(estatesSummary, emailTo);
    String content = templateEngine.process("email/portfolio-monthly-summary.html", context);
    sendEmail(
        "Měsíční přehled portfolia – " + monthName,
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public File getInvoiceFile(WatchDogMembershipEntity watchDogClient) {
    File invoiceFile = null;
    if (watchDogClient.getAgreedPrice().compareTo(BigDecimal.ZERO) > 0) {
      invoiceFile = generatePdfInvoice(watchDogClient);
    }
    return invoiceFile;
  }
}
