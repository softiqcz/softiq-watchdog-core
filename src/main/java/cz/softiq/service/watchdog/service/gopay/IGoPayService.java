package cz.softiq.service.watchdog.service.gopay;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.*;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import java.math.BigDecimal;
import java.util.List;

public interface IGoPayService extends IBeanProvider, IBaseService {

  private GoPayService getGoPayService() {
    return getBean(GoPayService.class);
  }

  default String createPayment(BigDecimal amount, String email, Boolean isNewUser) {
    return getGoPayService().createPayment(amount, email, isNewUser);
  }

  default String stopRecurringPayment(
      CoreClientEntity coreClientEntity, Boolean doDeactivateAccount) {
    return getGoPayService().stopRecurringPayment(coreClientEntity, doDeactivateAccount);
  }

  default void handleBypassPaywall(String email) {
    getGoPayService().handleBypassPaywall(email);
  }

  default void softChangeMembership(
      String email, MembershipEnum membership, BigDecimal amount, PromoCodeEnum promoCode) {
    getGoPayService().softChangeMembership(email, membership, amount, promoCode);
  }

  default String getPaymentStatus(String id, Boolean isNewClient) {
    return getGoPayService().getPaymentStatus(id, isNewClient);
  }

  default CoreClientEntity getUserByPaymentOrderId(String id) {
    return getGoPayService().getUserByPaymentOrderId(id);
  }

  default BigDecimal getAmountForOrderId(String id) {
    return getGoPayService().getAmountForOrderId(id);
  }

  default List<PaymentEntity> getAllActivePayments() {
    return getGoPayService().getAllActivePayments();
  }

  default Boolean verifyReccuringPaymentState(String id) {
    return getGoPayService().verifyReccuringPaymentState(id);
  }

  default PaymentEntity getRecurringPayment(String parentId, String id) {
    return getGoPayService().getRecurringPayment(parentId, id);
  }

  default String getPaymentMessage(String id) {
    return getGoPayService().getPaymentMessage(id);
  }
}
