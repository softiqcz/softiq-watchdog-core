package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.entity.WatchDogOldClientEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface IWatchDogAccountService extends IBeanProvider, IBaseService {

  default WatchDogMembershipEntity changeUserProfileInfo(
      String email, WatchDogMembershipEntity membership) {
    return getWatchDogAccountService().changeUserProfileInfo(email, membership);
  }

  default CoreClientEntity getCoreClientByEmail(String email) {
    return getWatchDogAccountService().getCoreClientByEmail(email);
  }

  default WatchDogMembershipEntity getWatchDogMembershipByCoreClient(CoreClientEntity coreClient) {
    return getWatchDogAccountService().getWatchDogMembership(coreClient);
  }

  default WatchDogMembershipEntity getWatchDogMembershipByEmail(String email) {
    return getWatchDogAccountService().getWatchDogMembership(email);
  }

  default Boolean isNewAccountEligibleForFreeMembership(CoreClientEntity coreClient) {
    return getWatchDogAccountService().isNewAccountEligibleForFreeMembership(coreClient);
  }

  default void sendResetPasswordToClientByEmail(String email) {
    getWatchDogAccountService().sendResetPasswordEmail(email);
  }

  default Boolean resetPassword(String email, String token, String password) {
    return getWatchDogAccountService().resetPassword(email, token, password);
  }

  default Boolean changePasswordForClient(Map<String, String> requestJson) {
    return getWatchDogAccountService().changePassword(requestJson);
  }

  default void deleteAccount(String email) {
    getWatchDogAccountService().deleteAccount(email);
  }

  default BigDecimal getAmountForMembershipAndStartMembershipChange(
      boolean isPaymentMethodChange,
      String email,
      MembershipEnum membership,
      PromoCodeEnum promoCode) {
    return getWatchDogAccountService()
        .startMembershipChange(isPaymentMethodChange, email, membership, promoCode);
  }

  default CoreClientEntity changeMembership(String id, Boolean stopPreviousPayment) {
    return getWatchDogAccountService().completeMembershipChange(id, stopPreviousPayment);
  }

  default String createNewUserFromRegistration(
      String recaptchaToken, String email, String password) {
    return getWatchDogAccountService().registerUser(email, password);
  }

  default boolean prolongUser(String paymentParentId, String latestId) {
    return getWatchDogAccountService().prolongRecurringMembership(paymentParentId, latestId);
  }

  default List<WatchDogOldClientEntity> getOldClients() {
    return getWatchDogAccountService().getOldClients();
  }

  default List<WatchDogMembershipEntity> getActiveWatchDogMemberships() {
    return getWatchDogAccountService().getActiveMemberships();
  }

  private WatchDogAccountService getWatchDogAccountService() {
    return getBean(WatchDogAccountService.class);
  }
}
