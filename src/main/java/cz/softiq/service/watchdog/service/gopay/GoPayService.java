package cz.softiq.service.watchdog.service.gopay;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.entity.PaymentEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GoPayService {
  private final GoPayComponent goPayComponent;

  String createPayment(BigDecimal amount, String email, Boolean isNewUser) {
    return goPayComponent.createPayment(amount, email, isNewUser);
  }

  String stopRecurringPayment(CoreClientEntity coreClientEntity, Boolean doDeactivateAccount) {
    return goPayComponent.stopRecurringPayment(coreClientEntity, doDeactivateAccount);
  }

  void handleBypassPaywall(String email) {
    goPayComponent.handleBypassPaywall(email);
  }

  void softChangeMembership(
      String email, MembershipEnum membership, BigDecimal amount, PromoCodeEnum promoCode) {
    goPayComponent.softChangeMembership(email, membership, amount, promoCode);
  }

  String getPaymentStatus(String id, Boolean isNewClient) {
    return goPayComponent.getPaymentStatus(id, isNewClient);
  }

  CoreClientEntity getUserByPaymentOrderId(String id) {
    return goPayComponent.getUserByPaymentOrderId(id);
  }

  BigDecimal getAmountForOrderId(String id) {
    return goPayComponent.getAmountForOrderId(id);
  }

  List<PaymentEntity> getAllActivePayments() {
    return goPayComponent.getAllActivePayments();
  }

  Boolean verifyReccuringPaymentState(String id) {
    return goPayComponent.verifyReccuringPaymentState(id);
  }

  PaymentEntity getRecurringPayment(String parentId, String id) {
    return goPayComponent.getRecurringPayment(parentId, id);
  }

  String getPaymentMessage(String id) {
    return goPayComponent.getPaymentMessage(id);
  }
}
