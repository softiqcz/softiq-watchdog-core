package cz.softiq.service.softiq.service.email;

import static cz.softiq.service.softiq.model.enums.ContextType.*;

import cz.softiq.core.model.TestReport;
import cz.softiq.core.model.builder.ContactRequest;
import cz.softiq.core.model.builder.Feature;
import cz.softiq.service.softiq.model.builder.ReviewRequest;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class EmailService {
  private final EmailWorkflowComponent component;

  void watchDogReportGrouped(List<List<WatchDogEstatesEntity>> groupedOffers, String emailTo) {
    component.watchDogReportGrouped(groupedOffers, emailTo);
  }

  void testSuiteReport(TestReport testReport) {
    component.testSuiteReport(testReport);
  }

  void coldEmail(String emailTo) {
    component.coldEmail(emailTo);
  }

  void missYouEmail(String emailTo) {
    component.missYouEmail(emailTo);
  }

  void newsUpdateEmail(String emailTo) {
    component.newsUpdateEmail(emailTo);
  }

  void sendEligiblePromoEmails() {
    component.sendEligiblePromoEmails();
  }

  boolean createWatchdogPromoEmail(String emailTo) {
    return component.createWatchdogPromoEmail(emailTo);
  }

  boolean createPortfolioPromoEmail(String emailTo) {
    return component.createPortfolioPromoEmail(emailTo);
  }

  boolean addTransactionsPromoEmail(String emailTo) {
    return component.addTransactionsPromoEmail(emailTo);
  }

  void useItEmail(String emailTo) {
    component.useItEmail(emailTo);
  }

  void contactRequest(ContactRequest contactRequest) {
    component.contactRequest(contactRequest);
  }

  void reviewRequest(ReviewRequest reviewRequest) {
    component.reviewRequest(reviewRequest);
  }

  void newFeature(Feature feature) {
    component.newFeature(feature);
  }

  void watchDogReportSingle(WatchDogEstatesEntity offer, String emailTo) {
    component.watchDogReportSingle(offer, emailTo);
  }

  void resetPassword(String url, String emailTo) {
    component.resetPassword(url, emailTo);
  }

  void sendInfoMessage(String emailTo) {
    component.sendInfoMessage(emailTo);
  }

  void registrationApologyEmail(String emailTo) {
    component.registrationApologyEmail(emailTo);
  }

  void feedbackRequestEmail(String emailTo) {
    component.feedbackRequestEmail(emailTo);
  }

  void deactivateAccount(String emailTo) {
    component.deactivateAccount(emailTo);
  }

  void changeMembership(
      String emailTo,
      MembershipEnum membership,
      String price,
      String oldPrice,
      WatchDogMembershipEntity watchDogClient) {
    component.changeMembership(emailTo, membership, price, oldPrice, watchDogClient);
  }

  void prolongUser(
      String emailTo,
      MembershipEnum membership,
      String price,
      WatchDogMembershipEntity watchDogClient) {
    component.prolongUser(emailTo, membership, price, watchDogClient);
  }

  void sendMembershipEnd3Days(
      WatchDogMembershipEntity watchDogMembershipEntity,
      List<WatchDogEstatesEntity> watchDogEstatesEntities) {
    component.sendMembershipEnd3Days(watchDogMembershipEntity, watchDogEstatesEntities);
  }

  void sendMembershipEnd0Days(
      WatchDogMembershipEntity watchDogMembershipEntity,
      List<WatchDogEstatesEntity> watchDogEstatesEntities) {
    component.sendMembershipEnd0Days(watchDogMembershipEntity, watchDogEstatesEntities);
  }

  void sendMembershipEndPayment(WatchDogMembershipEntity watchDogMembershipEntity) {
    component.sendMembershipEndPayment(watchDogMembershipEntity);
  }

  void sendMonthlyWatchdogSummaries() {
    component.sendMonthlyWatchdogSummaries();
  }

  void sendMonthlyPortfolioSummaries() {
    component.sendMonthlyPortfolioSummaries();
  }
}
