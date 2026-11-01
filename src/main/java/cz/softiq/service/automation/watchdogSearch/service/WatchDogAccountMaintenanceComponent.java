package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.service.watchdog.model.enums.MembershipEnum.BASIC;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.clients.CoreClientRepository;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.softiq.service.email.IEmailService;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.PaymentEntity;
import cz.softiq.service.watchdog.entity.WatchDogEntity;
import cz.softiq.service.watchdog.entity.WatchDogEstatesEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.TypeEnum;
import cz.softiq.service.watchdog.repository.WatchDogEstatesRepository;
import cz.softiq.service.watchdog.repository.WatchDogMembershipRepository;
import cz.softiq.service.watchdog.repository.WatchDogRepository;
import cz.softiq.service.watchdog.service.gopay.IGoPayService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class WatchDogAccountMaintenanceComponent
    implements IEmailService, IGoPayService, ICoreSoftiqService {
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final WatchDogRepository watchDogRepository;
  private final WatchDogEstatesRepository watchDogEstatesRepository;
  private final WatchDogMembershipRepository watchDogMembershipRepository;
  private final CoreClientRepository coreClientRepository;

  public void notifyBasicUsersAboutEndOfMembership() {
    List<WatchDogMembershipEntity> basicMemberships = getAllActiveBasicClients();
    LocalDateTime today = LocalDateTime.now();
    notifyUsersAboutMembershipEnd(
        basicMemberships, today.plusDays(3), this::sendMembershipEnd3Days);
    notifyUsersAboutMembershipEnd(basicMemberships, today, this::sendMembershipEnd0Days);
    disableWatchdogsExpiringOn(basicMemberships, today.minusDays(1));
  }

  public void sendNoActivityEmails() {
    for (CoreClientEntity client : coreClientRepository.findAll()) {
      List<WatchDogEntity> watchDogs =
          watchDogRepository.getAllByClientAndIsVisibleAndType(
              client, true, TypeEnum.AUTOMATION_WATCHDOG);
      long enabledCount =
          watchDogs.stream()
              .filter(watchDog -> Boolean.TRUE.equals(watchDog.getIsEnabled()))
              .count();
      WatchDogMembershipEntity membership =
          watchDogManagementComponent.getWatchDogMembership(client);
      if (membership != null
          && enabledCount == 0
          && membership.getMembership() == BASIC
          && ChronoUnit.DAYS.between(membership.getValidTo().toLocalDate(), LocalDate.now())
              == -9) {
        useItEmail(client.getEmail());
      }
    }
  }

  public void deleteInactiveAccounts() {
    List<WatchDogMembershipEntity> accounts = new ArrayList<>(getAllActiveBasicClients());
    accounts.addAll(getStoppedRecurringPaymentMemberships());
    accounts.stream()
        .filter(
            membership ->
                membership
                    .getValidTo()
                    .toLocalDate()
                    .isBefore(LocalDateTime.now().minusDays(7).toLocalDate()))
        .forEach(
            membership -> {
              watchDogManagementComponent.disableWatchdogs(membership);
              disableMembership(membership, true);
              watchDogManagementComponent.deleteAccount(membership.getClient());
            });
  }

  public void disableExpiredAccounts() {
    List<WatchDogMembershipEntity> accounts = getPendingAccounts();
    accounts.addAll(getExpiredBasicAccounts());
    accounts.forEach(watchDogManagementComponent::disableWatchdogs);
  }

  public void deleteOldChildWatchDogResults() {
    watchDogManagementComponent
        .getAllWatchDogChildren()
        .forEach(watchDogManagementComponent::deleteOldResults);
  }

  public void checkRecurringPayments() {
    LocalDateTime today = LocalDateTime.now();
    List<WatchDogMembershipEntity> usersToDisable =
        new ArrayList<>(getStoppedRecurringPaymentMemberships());
    usersToDisable.addAll(watchDogManagementComponent.getAllPastPayments(today));

    for (WatchDogMembershipEntity membership : usersToDisable) {
      String action = "NOTIFIED";
      if (!membership.getValidTo().isAfter(today.minusDays(14))) {
        action = "DELETED";
      } else if (!membership.getValidTo().isAfter(today.minusDays(3))) {
        disableMembership(membership, false);
        action = "DISABLED";
      }
      saveLog(
          new CoreLogEntity(
              null,
              LocalDate.now(),
              LocalDateTime.now(),
              "This account was " + action + " " + membership));
    }
  }

  private List<WatchDogMembershipEntity> getAllActiveBasicClients() {
    return watchDogMembershipRepository.findByIsActiveAndMembership(true, BASIC);
  }

  private void disableWatchdogsExpiringOn(
      List<WatchDogMembershipEntity> memberships, LocalDateTime targetDate) {
    memberships.stream()
        .filter(
            membership -> membership.getValidTo().toLocalDate().isEqual(targetDate.toLocalDate()))
        .forEach(watchDogManagementComponent::disableWatchdogs);
  }

  private List<WatchDogMembershipEntity> getExpiredBasicAccounts() {
    LocalDate weekOld = LocalDate.now().minusDays(7);
    return watchDogMembershipRepository.findAllByMembership(BASIC).stream()
        .filter(membership -> membership.getValidTo().toLocalDate().isBefore(weekOld))
        .toList();
  }

  private List<WatchDogMembershipEntity> getPendingAccounts() {
    LocalDate weekOld = LocalDate.now().minusDays(7);
    return new ArrayList<>(
        watchDogMembershipRepository.findAllByMembership(MembershipEnum.PENDING).stream()
            .filter(membership -> membership.getValidTo().toLocalDate().isEqual(weekOld))
            .toList());
  }

  private void notifyUsersAboutMembershipEnd(
      List<WatchDogMembershipEntity> memberships,
      LocalDateTime targetDate,
      BiConsumer<WatchDogMembershipEntity, List<WatchDogEstatesEntity>> emailAction) {
    memberships.stream()
        .filter(
            membership -> membership.getValidTo().toLocalDate().isEqual(targetDate.toLocalDate()))
        .forEach(
            membership -> {
              List<WatchDogEstatesEntity> offers =
                  watchDogEstatesRepository.findWatchDogEstatesEntitiesByClientAndIsReported(
                      membership.getClient(), true);
              emailAction.accept(membership, offers);
            });
  }

  private void disableMembership(WatchDogMembershipEntity membership, boolean changeDate) {
    membership.setPendingMembership(membership.getMembership());
    membership.setMembership(BASIC);
    if (changeDate) {
      LocalDateTime newDate = LocalDateTime.now();
      membership.setValidTo(newDate);
      membership.setValidToFormatted(
          newDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    }
    watchDogMembershipRepository.save(membership);
  }

  private List<WatchDogMembershipEntity> getStoppedRecurringPaymentMemberships() {
    List<WatchDogMembershipEntity> memberships = new ArrayList<>();
    for (PaymentEntity payment : getAllActivePayments()) {
      if (!verifyReccuringPaymentState(payment.getOrderId())) {
        memberships.add(watchDogManagementComponent.getWatchDogMembership(payment.getClient()));
      }
    }
    return memberships;
  }
}
