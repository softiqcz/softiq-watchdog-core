package cz.softiq.service.automation.watchdogSearch.service;

import static cz.softiq.service.softiq.model.constants.MessageConstants.DUPLICATE_USER;
import static cz.softiq.service.watchdog.model.enums.PaymentStateEnum.CREATED;
import static java.lang.Boolean.TRUE;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.clients.CoreClientRepository;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.softiq.service.email.IEmailService;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PaymentStateEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import cz.softiq.service.watchdog.repository.PaymentRepository;
import cz.softiq.service.watchdog.repository.WatchDogMembershipRepository;
import cz.softiq.service.watchdog.service.gopay.IGoPayService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
class WatchDogMembershipWorkflowComponent
    implements IGoPayService, ICoreSoftiqService, IEmailService {
  private final WatchDogManagementComponent watchDogManagementComponent;
  private final CoreClientRepository coreClientRepository;
  private final PaymentRepository paymentRepository;
  private final WatchDogMembershipRepository watchDogMembershipRepository;

  public Boolean changePassword(Map<String, String> requestJson) {
    String email = requestJson.get("email");
    String oldPassword = requestJson.get("oldPassword");
    String newPassword = requestJson.get("newPassword");

    CoreClientEntity client = watchDogManagementComponent.getCoreClient(email);
    PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    if (!passwordEncoder.matches(oldPassword, client.getPassword())) {
      return Boolean.FALSE;
    }
    client.setPassword(passwordEncoder.encode(newPassword));
    coreClientRepository.save(client);
    return Boolean.TRUE;
  }

  public synchronized BigDecimal startMembershipChange(
      boolean isPaymentMethodChange,
      String email,
      MembershipEnum membership,
      PromoCodeEnum promoCode) {
    CoreClientEntity client = watchDogManagementComponent.getCoreClient(email);
    validatePromoAvailability(client, promoCode);
    BigDecimal amount =
        isPaymentMethodChange
            ? watchDogManagementComponent.getFixedPrice(client)
            : watchDogManagementComponent.getPrice(
                membership, watchDogManagementComponent.getWatchDogMembership(client), promoCode);
    watchDogManagementComponent.preSaveMembership(email, membership, promoCode);
    return amount;
  }

  private void validatePromoAvailability(CoreClientEntity client, PromoCodeEnum promoCode) {
    if (promoCode == null || promoCode.getMaxRedemptions() == null) {
      return;
    }
    boolean alreadyRedeemed =
        watchDogMembershipRepository.existsByPromoCodeAndClient(promoCode, client);
    if (!alreadyRedeemed
        && watchDogMembershipRepository.countRedeemersByPromoCode(promoCode)
            >= promoCode.getMaxRedemptions()) {
      throw new SoftiqException("Promo kód již využil maximální počet uživatelů");
    }
  }

  public CoreClientEntity completeMembershipChange(String paymentId, Boolean stopPreviousPayment) {
    String email = "unknown";
    String state = getPaymentStatus(paymentId, !stopPreviousPayment);
    if (PaymentStateEnum.PAID.getState().equals(state)) {
      CoreClientEntity client = getUserByPaymentOrderId(paymentId);
      WatchDogMembershipEntity membership =
          watchDogManagementComponent.getWatchDogMembership(client);
      boolean canProceed = true;
      if (membership == null) {
        membership = watchDogManagementComponent.getNewWatchDogClient(client);
      } else if (membership.getPendingMembership() == null) {
        canProceed = false;
      }

      if (canProceed) {
        prolongUser(
            client.getEmail(),
            membership.getMembership(),
            membership.getAgreedPrice().toString(),
            membership);
        email = client.getEmail();
        try {
          watchDogManagementComponent.enableAccount(email);
          BigDecimal amount = getAmountForOrderId(paymentId);
          watchDogManagementComponent.changeMembership(
              email, membership.getPendingMembership(), amount, stopPreviousPayment);
          return client;
        } catch (Exception exception) {
          throw membershipChangeException(
              "Chyba při změně členství: ", email, exception.getMessage());
        }
      }
      return null;
    }

    if (CREATED.getState().equals(state)) {
      String errorMessage = "Platba nebyla dokončena: " + email + " " + state;
      logError(errorMessage);
      return null;
    }

    throw membershipChangeException("Chyba při změně členství2: ", email, state);
  }

  public String registerUser(String email, String password) {
    String errorMessage = null;
    try {
      CoreClientEntity client = watchDogManagementComponent.getCoreClient(email);
      if (client == null) {
        watchDogManagementComponent.createNewClient(email, password, false);
      } else if (client.isEnabled()) {
        errorMessage = DUPLICATE_USER;
      } else {
        watchDogManagementComponent.createNewClient(email, password, true);
      }
    } catch (DataIntegrityViolationException | ConstraintViolationException exception) {
      errorMessage = DUPLICATE_USER;
    } catch (Exception exception) {
      errorMessage = "Chyba při registraci: " + exception.getMessage();
    }
    return errorMessage;
  }

  public synchronized boolean prolongRecurringMembership(String paymentParentId, String latestId) {
    var childPayment = getRecurringPayment(paymentParentId, latestId);
    var existingPayments = paymentRepository.findByOrderId(latestId);
    if (!existingPayments.isEmpty()
        && PaymentStateEnum.PAID.getState().equals(existingPayments.getFirst().getState())) {
      return true;
    }
    if (!existingPayments.isEmpty()) {
      childPayment.setId(existingPayments.getFirst().getId());
    }
    if (!PaymentStateEnum.PAID.getState().equals(childPayment.getState())) {
      paymentRepository.save(childPayment);
      return false;
    }
    var oldPayment = paymentRepository.findByOrderId(paymentParentId).getFirst();
    // Older callbacks stored the last handled child ID on the parent payment.
    if (latestId.equals(oldPayment.getOrderNumber())) {
      paymentRepository.save(childPayment);
      return true;
    }
    var client = oldPayment.getClient();
    WatchDogMembershipEntity membership =
        watchDogMembershipRepository.findByIsActiveAndClient(TRUE, client).getFirst();
    LocalDateTime newDate = LocalDateTime.now().plusMonths(1);
    membership.setValidTo(newDate);
    membership.setValidToFormatted(newDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
    watchDogMembershipRepository.save(membership);
    prolongUser(
        client.getEmail(),
        membership.getMembership(),
        childPayment.getAmount().divide(BigDecimal.valueOf(100)).toPlainString(),
        membership);
    oldPayment.setState(PaymentStateEnum.RECURRENCE_OK.getState());
    oldPayment.setUpdatedDate(LocalDateTime.now());
    oldPayment.setOrderNumber(latestId);
    paymentRepository.save(oldPayment);
    paymentRepository.save(childPayment);
    return true;
  }

  private SoftiqException membershipChangeException(String prefix, String email, String detail) {
    String errorMessage = prefix + email + " " + detail;
    logError(errorMessage);
    return new SoftiqException(errorMessage);
  }

  private void logError(String errorMessage) {
    saveLog(new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), errorMessage));
    log.error(errorMessage);
  }
}
