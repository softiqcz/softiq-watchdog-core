package cz.softiq.service.automation.watchdogSearch.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WatchDogMaintenanceService {
  private final WatchDogAccountMaintenanceComponent accountMaintenanceComponent;

  void notifyBasicUsersAboutEndOfMembership() {
    accountMaintenanceComponent.notifyBasicUsersAboutEndOfMembership();
  }

  void deleteInactiveAccounts() {
    accountMaintenanceComponent.deleteInactiveAccounts();
  }

  void sendNoActivityEmails() {
    accountMaintenanceComponent.sendNoActivityEmails();
  }

  void disableExpiredAccounts() {
    accountMaintenanceComponent.disableExpiredAccounts();
  }

  void deleteOldChildWatchDogResults() {
    accountMaintenanceComponent.deleteOldChildWatchDogResults();
  }

  void checkRecurringPayments() {
    accountMaintenanceComponent.checkRecurringPayments();
  }
}
