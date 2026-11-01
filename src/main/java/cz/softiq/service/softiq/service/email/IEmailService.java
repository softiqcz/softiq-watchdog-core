package cz.softiq.service.softiq.service.email;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.model.TestReport;
import cz.softiq.core.model.builder.ContactRequest;
import cz.softiq.core.model.builder.Feature;
import cz.softiq.service.softiq.model.builder.ReviewRequest;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import java.util.List;

public interface IEmailService extends IBeanProvider, IBaseService {

  default void coldEmail(String emailTo) {
    //    getEmailService().coldEmail(emailTo);
  }

  default void missYouEmail(String emailTo) {
    getEmailService().missYouEmail(emailTo);
  }

  default void newsUpdateEmail(String emailTo) {
    getEmailService().newsUpdateEmail(emailTo);
  }

  default void sendEligiblePromoEmails() {
    getEmailService().sendEligiblePromoEmails();
  }

  default void contactRequest(ContactRequest contactRequest) {
    getEmailService().contactRequest(contactRequest);
  }

  default void reviewRequest(ReviewRequest reviewRequest) {
    getEmailService().reviewRequest(reviewRequest);
  }

  default void useItEmail(String emailTo) {
    getEmailService().useItEmail(emailTo);
  }

  default void testSuiteReport(TestReport testReport) {
    getEmailService().testSuiteReport(testReport);
  }

  default void newFeatureEmail(Feature feature) {
    getEmailService().newFeature(feature);
  }

  default void watchDogReport(List<List<WatchDogEstatesEntity>> groupedOffers, String emailTo) {
    getEmailService().watchDogReportGrouped(groupedOffers, emailTo);
  }

  default void watchDogReportSingle(WatchDogEstatesEntity offer, String emailTo) {
    getEmailService().watchDogReportSingle(offer, emailTo);
  }

  default void resetPasswordEmail(String url, String emailTo) {
    getEmailService().resetPassword(url, emailTo);
  }

  default void sendInfoMessage(String emailTo) {
    getEmailService().sendInfoMessage(emailTo);
  }

  default void registrationApologyEmail(String emailTo) {
    getEmailService().registrationApologyEmail(emailTo);
  }

  default void feedbackRequestEmail(String emailTo) {
    getEmailService().feedbackRequestEmail(emailTo);
  }

  default void deactivateAccount(String emailTo) {
    getEmailService().deactivateAccount(emailTo);
  }

  default void changeMembership(
      String emailTo,
      MembershipEnum membership,
      String price,
      String oldPrice,
      WatchDogMembershipEntity watchDogClient) {
    getEmailService().changeMembership(emailTo, membership, price, oldPrice, watchDogClient);
  }

  default void prolongUser(
      String emailTo,
      MembershipEnum membership,
      String price,
      WatchDogMembershipEntity watchDogClient) {
    getEmailService().prolongUser(emailTo, membership, price, watchDogClient);
  }

  default void sendMembershipEnd3Days(
      WatchDogMembershipEntity watchDogMembershipEntity,
      List<WatchDogEstatesEntity> watchDogEstatesEntities) {
    getEmailService().sendMembershipEnd3Days(watchDogMembershipEntity, watchDogEstatesEntities);
  }

  default void sendMembershipEnd0Days(
      WatchDogMembershipEntity watchDogMembershipEntity,
      List<WatchDogEstatesEntity> watchDogEstatesEntities) {
    getEmailService().sendMembershipEnd0Days(watchDogMembershipEntity, watchDogEstatesEntities);
  }

  default void sendMembershipEndPayment(WatchDogMembershipEntity watchDogMembershipEntity) {
    getEmailService().sendMembershipEndPayment(watchDogMembershipEntity);
  }

  default void sendMonthlyWatchdogSummaries() {
    getEmailService().sendMonthlyWatchdogSummaries();
  }

  default void sendMonthlyPortfolioSummaries() {
    getEmailService().sendMonthlyPortfolioSummaries();
  }

  private EmailService getEmailService() {
    return getBean(EmailService.class);
  }
}
