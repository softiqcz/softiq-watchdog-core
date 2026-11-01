package cz.softiq.service.softiq.service.unsubscribe;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;

public interface IUnsubscribeService extends IBeanProvider, IBaseService {

  default void unsubscribeUserByEmail(String email) {
    getUnsubscribeService().unsubscribeUserByEmail(email);
  }

  default boolean canSendEmail(String email) {
    return getUnsubscribeService().canSendEmail(email);
  }

  default String generateUnsubscribeLink(String email) {
    return getUnsubscribeService().generateUnsubscribeLink(email);
  }

  private UnsubscribeService getUnsubscribeService() {
    return getBean(UnsubscribeService.class);
  }
}
