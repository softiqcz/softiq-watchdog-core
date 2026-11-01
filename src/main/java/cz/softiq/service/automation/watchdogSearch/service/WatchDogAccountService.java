package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.entity.WatchDogOldClientEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchDogAccountService {
  private final WatchDogManagementComponent managementComponent;
  private final WatchDogMembershipWorkflowComponent membershipWorkflowComponent;
  private final WatchDogPersistenceComponent persistenceComponent;

  WatchDogMembershipEntity changeUserProfileInfo(
      String email, WatchDogMembershipEntity membership) {
    return managementComponent.changeUserProfileInfo(email, membership);
  }

  CoreClientEntity getCoreClientByEmail(String email) {
    return managementComponent.getCoreClient(email);
  }

  WatchDogMembershipEntity getWatchDogMembership(CoreClientEntity coreClient) {
    return managementComponent.getWatchDogMembership(coreClient);
  }

  WatchDogMembershipEntity getWatchDogMembership(String email) {
    return managementComponent.getWatchDogMembership(email);
  }

  Boolean isNewAccountEligibleForFreeMembership(CoreClientEntity coreClient) {
    return managementComponent.getIfIsNewEmail(coreClient.getEmail());
  }

  void sendResetPasswordEmail(String email) {
    managementComponent.sendEmailToResetPw(email);
  }

  Boolean resetPassword(String email, String token, String password) {
    return managementComponent.changePassword(email, token, password);
  }

  Boolean changePassword(Map<String, String> requestJson) {
    return membershipWorkflowComponent.changePassword(requestJson);
  }

  void deleteAccount(String email) {
    managementComponent.deleteAccount(managementComponent.getCoreClient(email));
  }

  BigDecimal startMembershipChange(
      boolean isPaymentMethodChange,
      String email,
      MembershipEnum membership,
      PromoCodeEnum promoCode) {
    return membershipWorkflowComponent.startMembershipChange(
        isPaymentMethodChange, email, membership, promoCode);
  }

  CoreClientEntity completeMembershipChange(String id, Boolean stopPreviousPayment) {
    return membershipWorkflowComponent.completeMembershipChange(id, stopPreviousPayment);
  }

  String registerUser(String email, String password) {
    return membershipWorkflowComponent.registerUser(email, password);
  }

  boolean prolongRecurringMembership(String paymentParentId, String latestId) {
    return membershipWorkflowComponent.prolongRecurringMembership(paymentParentId, latestId);
  }

  List<WatchDogOldClientEntity> getOldClients() {
    return persistenceComponent.getOldClients();
  }

  List<WatchDogMembershipEntity> getActiveMemberships() {
    return persistenceComponent.getActiveMemberships();
  }
}
