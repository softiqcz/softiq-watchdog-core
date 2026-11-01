package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;

public interface IWatchDogMaintenanceService extends IBeanProvider, IBaseService {

  default void notifyBasicUsersAboutEndOfMembership() {
    getWatchDogMaintenanceService().notifyBasicUsersAboutEndOfMembership();
  }

  default void deleteInActiveAccounts() {
    getWatchDogMaintenanceService().deleteInactiveAccounts();
  }

  default void noActivityMail() {
    getWatchDogMaintenanceService().sendNoActivityEmails();
  }

  default void deleteAccountScheduled() {
    getWatchDogMaintenanceService().disableExpiredAccounts();
  }

  default void deleteOldWatchdogChildrenEstates() {
    getWatchDogMaintenanceService().deleteOldChildWatchDogResults();
  }

  default void checkPaymentStillLive() {
    getWatchDogMaintenanceService().checkRecurringPayments();
  }

  private WatchDogMaintenanceService getWatchDogMaintenanceService() {
    return getBean(WatchDogMaintenanceService.class);
  }
}
