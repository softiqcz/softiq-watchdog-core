package cz.softiq.service.softiq.service.email;

import static cz.softiq.core.model.enums.ResultEnum.PASSED;
import static cz.softiq.service.softiq.model.enums.ContextType.*;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.email.CoreEmailComponent;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.email.CoreReportingEntity;
import cz.softiq.core.email.ICoreEmailService;
import cz.softiq.core.model.TestReport;
import cz.softiq.core.model.builder.ContactRequest;
import cz.softiq.core.model.builder.Feature;
import cz.softiq.core.model.enums.CoreContextType;
import cz.softiq.core.model.enums.ResultEnum;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.automation.watchdogSearch.service.WatchdogResultAnalysisComponent;
import cz.softiq.service.realiq.repository.ClientEstateRepository;
import cz.softiq.service.realiq.repository.TransactionRepository;
import cz.softiq.service.realiq.service.portfolio.PortfolioAnalysisComponent;
import cz.softiq.service.softiq.model.builder.ReviewRequest;
import cz.softiq.service.softiq.repository.PromoEmailRepository;
import cz.softiq.service.softiq.service.reporting.IReportingService;
import cz.softiq.service.softiq.service.unsubscribe.IUnsubscribeService;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.ClientWatchdogsSummary;
import cz.softiq.service.watchdog.model.WatchdogMonthlySummary;
import cz.softiq.service.watchdog.model.WatchdogWithSummary;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.repository.WatchDogMembershipRepository;
import cz.softiq.service.watchdog.repository.WatchDogRepository;
import cz.softiq.service.watchdog.service.invoice.IInvoiceService;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailWorkflowComponent
    implements IBaseService,
        IWatchDogAccountService,
        IInvoiceService,
        IReportingService,
        ICoreEmailService,
        ICoreSoftiqService,
        IUnsubscribeService {
  private static final int PROMO_EMAIL_COOLDOWN_DAYS = 3;

  private final TemplateEngine templateEngine;
  private final EmailComponent emailComponent;
  private final CoreEmailComponent coreEmailComponent;
  private final WatchDogMembershipRepository membershipRepository;
  private final WatchDogRepository watchDogRepository;
  private final ClientEstateRepository estateRepository;
  private final TransactionRepository transactionRepository;
  private final PromoEmailRepository promoEmailRepository;
  private final WatchdogResultAnalysisComponent watchdogResultAnalysisComponent;
  private final PortfolioAnalysisComponent portfolioAnalysisComponent;

  @Value("${spring.mail.username}")
  private String adminEmail;

  public void watchDogReportGrouped(
      List<List<WatchDogEstatesEntity>> groupedOffers, String emailTo) {
    Context context = emailComponent.getContext(GROUPED_OFFERS, groupedOffers);
    String content = emailComponent.renderWatchDogReport(context);
    sendEmail("Report hlídacího psa", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void testSuiteReport(TestReport testReport) {
    ResultEnum result = testReport.getResult();
    String subject = result + " | " + testReport.getName();

    testReport.setEmailSubject(subject);
    Context context = emailComponent.getContext(TEST_REPORT, testReport);
    String content = templateEngine.process("email/test-suite-report.html", context);
    saveTestReport(testReport);
    if (!result.equals(PASSED)) {
      sendEmail(subject, content, testReport.getEmailTo(), adminEmail, "SOFTIQ", null, null);
    } else {
      CoreReportingEntity reportEntity =
          new CoreReportingEntity(
              null, LocalDate.now(), LocalDateTime.now(), adminEmail, adminEmail, subject, content);
      saveEmailReport(reportEntity);
    }
  }

  public void coldEmail(String emailTo) {
    Context ctx = new Context();
    String content = templateEngine.process("email/business-proposal-2.html", ctx);
    if (canSendEmail(emailTo)) {
      sendEmail(
          "Hlídací pes na nemovitosti", content, emailTo, adminEmail, "Martin Švejda", null, null);
    }
  }

  public void missYouEmail(String emailTo) {
    Context context = emailComponent.getContext(MISS_YOU, emailTo);
    String content = templateEngine.process("email/miss-you-proposal.html", context);
    if (canSendEmail(emailTo)) {
      sendEmail(
          "Chybíte nám. Co takhle slevový kód?",
          content,
          emailTo,
          adminEmail,
          "SOFTIQ",
          null,
          null);
    }
  }

  public void newsUpdateEmail(String emailTo) {
    emailComponent.sendPortfolioNewsEmail(emailTo);
  }

  public void sendEligiblePromoEmails() {
    Set<Integer> processedClientIds = new HashSet<>();

    for (WatchDogMembershipEntity membership : membershipRepository.findAllByIsActive(true)) {
      CoreClientEntity client = membership.getClient();
      if (!processedClientIds.add(client.getId())) {
        continue;
      }
      boolean hasWatchDogs = watchDogRepository.existsByClient(client);
      boolean hasPortfolio = estateRepository.existsByClient(client);

      if (!hasWatchDogs && createWatchdogPromoEmail(client.getEmail())) {
        continue;
      }
      if (!hasPortfolio) {
        createPortfolioPromoEmail(client.getEmail());
      } else if (!transactionRepository.existsByEstateClient(client)) {
        addTransactionsPromoEmail(client.getEmail());
      }
    }
  }

  public boolean createWatchdogPromoEmail(String emailTo) {
    CoreClientEntity client = getCoreClientByEmail(emailTo);
    if (client == null) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();
    if (promoEmailRepository.existsByClientAndSentAtAfter(
        client, now.minusDays(PROMO_EMAIL_COOLDOWN_DAYS))) {
      return false;
    }
    return emailComponent.sendCreateWatchdogPromoEmail(client, now.minusMonths(1), now);
  }

  public boolean createPortfolioPromoEmail(String emailTo) {
    CoreClientEntity client = getCoreClientByEmail(emailTo);
    if (client == null) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();
    if (promoEmailRepository.existsByClientAndSentAtAfter(
        client, now.minusDays(PROMO_EMAIL_COOLDOWN_DAYS))) {
      return false;
    }
    return emailComponent.sendCreatePortfolioPromoEmail(client, now.minusMonths(1), now);
  }

  public boolean addTransactionsPromoEmail(String emailTo) {
    CoreClientEntity client = getCoreClientByEmail(emailTo);
    if (client == null) {
      return false;
    }

    LocalDateTime now = LocalDateTime.now();
    if (promoEmailRepository.existsByClientAndSentAtAfter(
        client, now.minusDays(PROMO_EMAIL_COOLDOWN_DAYS))) {
      return false;
    }
    return emailComponent.sendAddTransactionsPromoEmail(client, now.minusMonths(1), now);
  }

  public void useItEmail(String emailTo) {
    CoreClientEntity client = getCoreClientByEmail(emailTo);
    if (client == null) {
      return;
    }

    LocalDateTime now = LocalDateTime.now();
    if (promoEmailRepository.existsByClientAndSentAtAfter(
        client, now.minusDays(PROMO_EMAIL_COOLDOWN_DAYS))) {
      return;
    }

    emailComponent.sendNoActivityPromoEmail(client, now.minusMonths(1), now);
  }

  public void contactRequest(ContactRequest contactRequest) {
    Context context =
        coreEmailComponent.getContext(CoreContextType.CONTACT_REQUEST, contactRequest);
    String content = templateEngine.process("email/contact.html", context);
    sendEmail(
        "Zpráva z webu",
        content,
        contactRequest.getOwnerEmail(),
        adminEmail,
        contactRequest.getPersonalFrom(),
        contactRequest.getClientEmail(),
        null);
  }

  public void reviewRequest(ReviewRequest reviewRequest) {
    Context context = new Context();
    context.setVariable("reviewRequest", reviewRequest);
    String content = templateEngine.process("email/review.html", context);
    sendEmail("Nová recenze z webu", content, adminEmail, adminEmail, "SOFTIQ", null, null);
  }

  public void newFeature(Feature feature) {
    Context ctx = coreEmailComponent.getContext(CoreContextType.FEATURE, feature);
    String content = templateEngine.process("email/feature-proposal.html", ctx);
    sendEmail(
        "Nový požadavek na " + feature.getType(),
        content,
        adminEmail,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public void watchDogReportSingle(WatchDogEstatesEntity offer, String emailTo) {
    Context context = emailComponent.getContext(OFFER, offer);
    String content = emailComponent.renderWatchDogReport(context);
    sendEmail(
        context.getVariable("subject").toString(),
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public void resetPassword(String url, String emailTo) {
    Context context = emailComponent.getContext(PASSWORD_RESET, url);
    String content = templateEngine.process("email/password-reset.html", context);
    sendEmail("Obnova hesla", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void sendInfoMessage(String emailTo) {
    String content = templateEngine.process("email/info-message.html", new Context());
    sendEmail("Vaše členství je u konce!", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void registrationApologyEmail(String emailTo) {
    String content = templateEngine.process("email/registration-apology.html", new Context());
    sendEmail("Registrace", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void feedbackRequestEmail(String emailTo) {
    if (!canSendEmail(emailTo)) {
      return;
    }

    Context context = new Context();
    context.setVariable("unsubscribeUrl", generateUnsubscribeLink(emailTo));
    String content = templateEngine.process("email/feedback-request.html", context);
    sendEmail("Napište nám recenzi", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void deactivateAccount(String emailTo) {
    String content = templateEngine.process("email/deactivate-account.html", new Context());
    sendEmail("Potvrzení deaktivace účtu", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void changeMembership(
      String emailTo,
      MembershipEnum membership,
      String price,
      String oldPrice,
      WatchDogMembershipEntity watchDogClient) {
    Context context =
        emailComponent.getContext(MEMBERSHIP_CHANGE, membership, price, oldPrice, watchDogClient);
    String content = templateEngine.process("email/change-membership.html", context);
    sendEmail(
        "Potvrzení objednávky předplatného", content, emailTo, adminEmail, "SOFTIQ", null, null);
  }

  public void prolongUser(
      String emailTo,
      MembershipEnum membership,
      String price,
      WatchDogMembershipEntity watchDogClient) {
    Context context =
        emailComponent.getContext(MEMBERSHIP_PROLONG, membership, price, watchDogClient);
    String content = templateEngine.process("email/prolong-user.html", context);
    File invoiceFile = emailComponent.getInvoiceFile(watchDogClient);
    sendEmailWithPdf(
        "Faktura za měsíční předplatné",
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        "faktura",
        invoiceFile);
    sendEmailWithPdf(
        "Vydaná faktura za měsíční předplatné",
        content,
        adminEmail,
        adminEmail,
        "SOFTIQ",
        null,
        "faktura",
        invoiceFile);
  }

  public void sendMembershipEnd3Days(
      WatchDogMembershipEntity watchDogMembershipEntity,
      List<WatchDogEstatesEntity> watchDogEstatesEntities) {
    Context context =
        emailComponent.getContext(
            EXPIRY_REMINDER,
            watchDogMembershipEntity,
            watchDogEstatesEntities,
            3,
            "Nezapomeňte upgradovat členství");
    String content = templateEngine.process("email/watchdog-membership-end-soon.html", context);
    var emailTo = context.getVariable("clientName").toString();
    sendEmail(
        context.getVariable("title").toString(),
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public void sendMembershipEnd0Days(
      WatchDogMembershipEntity watchDogMembershipEntity,
      List<WatchDogEstatesEntity> watchDogEstatesEntities) {
    Context context =
        emailComponent.getContext(
            EXPIRY_REMINDER,
            watchDogMembershipEntity,
            watchDogEstatesEntities,
            0,
            "Nezapomeňte dnes upgradovat členství");
    String content = templateEngine.process("email/watchdog-membership-end-now.html", context);
    var emailTo = context.getVariable("clientName").toString();
    sendEmail(
        context.getVariable("title").toString(),
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public void sendMembershipEndPayment(WatchDogMembershipEntity watchDogMembershipEntity) {
    Context context =
        emailComponent.getContext(PAYMENT_FAILED, watchDogMembershipEntity, "Platba selhala");
    String content =
        templateEngine.process("email/watchdog-membership-payment-failed.html", context);
    var emailTo = context.getVariable("clientName").toString();
    sendEmail(
        context.getVariable("title").toString(),
        content,
        emailTo,
        adminEmail,
        "SOFTIQ",
        null,
        null);
  }

  public void sendMonthlyWatchdogSummaries() {
    log.info("Starting sendMonthlyWatchdogSummaries");
    LocalDate lastMonth = LocalDate.now().minusMonths(1);

    // Group watchdogs by client
    Map<CoreClientEntity, List<WatchDogEntity>> watchdogsByClient = new HashMap<>();

    for (WatchDogEntity watchdog : watchDogRepository.findAll()) {
      if (watchdog.getClient() == null || watchdog.getClient().getEmail() == null) {
        continue;
      }

      // Only include visible and enabled watchdogs
      if (!watchdog.getIsVisible() || !watchdog.getIsEnabled()) {
        continue;
      }

      watchdogsByClient.computeIfAbsent(watchdog.getClient(), k -> new ArrayList<>()).add(watchdog);
    }

    // Send one email per client with all their watchdogs
    for (Map.Entry<CoreClientEntity, List<WatchDogEntity>> entry : watchdogsByClient.entrySet()) {
      CoreClientEntity client = entry.getKey();
      List<WatchDogEntity> clientWatchdogs = entry.getValue();

      try {
        String clientEmail = client.getEmail();
        if (!canSendEmail(clientEmail)) {
          continue;
        }

        ClientWatchdogsSummary clientSummary = new ClientWatchdogsSummary();

        for (WatchDogEntity watchdog : clientWatchdogs) {
          WatchdogMonthlySummary summary =
              watchdogResultAnalysisComponent.analyzeMonthlySummary(watchdog, lastMonth);

          if (summary.getCurrentMonthOfferCount() > 0) {
            WatchdogWithSummary ws = new WatchdogWithSummary(watchdog, summary);
            clientSummary.getWatchdogSummaries().add(ws);
          }
        }

        if (!clientSummary.hasWatchdogs()) {
          continue;
        }

        emailComponent.sendClientWatchdogsMonthlySummary(clientSummary, clientEmail);
      } catch (Exception e) {
        saveLog(
            new CoreLogEntity(
                null,
                LocalDate.now(),
                LocalDateTime.now(),
                "Failed to send monthly watchdog summary for client %s: %s"
                    .formatted(client.getId(), e.getMessage())));
      }
    }
  }

  public void sendMonthlyPortfolioSummaries() {
    log.info("Starting sendMonthlyPortfolioSummaries");
    LocalDate lastMonth = LocalDate.now().minusMonths(1);

    List<CoreClientEntity> clientsWithEstates = estateRepository.findAllClientsWithEstates();

    Set<Integer> processedClientIds = new HashSet<>();

    for (CoreClientEntity client : clientsWithEstates) {
      try {
        // Safety check: skip if already processed
        if (!processedClientIds.add(client.getId())) {
          continue;
        }

        String clientEmail = client.getEmail();
        if (clientEmail == null || clientEmail.isBlank()) {
          continue;
        }

        if (!canSendEmail(clientEmail)) {
          continue;
        }

        cz.softiq.service.realiq.model.PortfolioEstatesSummary estatesSummary =
            portfolioAnalysisComponent.analyzeMonthlyTransactionsByEstate(client, lastMonth);

        if (estatesSummary.getTotalSummary().getTotalTransactionCount() == 0) {
          continue;
        }

        emailComponent.sendPortfolioEstatesMonthlySummary(estatesSummary, clientEmail);
      } catch (Exception e) {
        saveLog(
            new CoreLogEntity(
                null,
                LocalDate.now(),
                LocalDateTime.now(),
                "Failed to send monthly portfolio summary for client %s: %s"
                    .formatted(client.getId(), e.getMessage())));
      }
    }
  }
}
