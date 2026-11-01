package cz.softiq.service.watchdog.service.gopay;

import static cz.softiq.core.configuration.profile.Profile.DEV;
import static cz.softiq.service.watchdog.model.enums.PaymentStateEnum.*;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.core.configuration.profile.ProfileChecker;
import cz.softiq.core.email.CoreLogEntity;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.PaymentEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.model.enums.PaymentStateEnum;
import cz.softiq.service.watchdog.model.enums.PromoCodeEnum;
import cz.softiq.service.watchdog.repository.PaymentRepository;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
class GoPayComponent implements IBaseService, IBeanProvider, ICoreSoftiqService {

  private static final Set<String> TEST_PAYMENT_IDS = Set.of("1", "2", "3", "4", "5");

  private final WatchDogManagementComponent watchDogManagementComponent;
  private final ProfileChecker profileChecker;

  @Value("${gopay.url}")
  private String BASE_URL;

  @Value("${gopay.client.id}")
  private String CLIENT_ID;

  @Value("${gopay.client.secret}")
  private String CLIENT_SECRET;

  @Value("${gopay.go.id}")
  private String GO_ID;

  private final PaymentRepository paymentRepository;

  public String createPayment(BigDecimal amount, String email, Boolean isNewUser) {
    return createPayment(
        getAccessToken(),
        generateOrderNumber(),
        amount.multiply(BigDecimal.valueOf(100)),
        email,
        isNewUser);
  }

  public String stopRecurringPayment(CoreClientEntity client, Boolean deactivate) {
    return stopRecurringPayment(getAccessToken(), client, deactivate);
  }

  public String getPaymentStatus(String id, Boolean isNewClient) {
    return getPaymentStatus(getAccessToken(), id, isNewClient);
  }

  public Boolean verifyReccuringPaymentState(String id) {
    if (TEST_PAYMENT_IDS.contains(id)) {
      return true;
    }
    return verifyReccuringPaymentState(getAccessToken(), id);
  }

  public String getAccessToken() {
    Response response =
        RestAssured.given()
            .contentType(ContentType.URLENC)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .formParam("scope", "payment-create")
            .formParam("grant_type", "client_credentials")
            .auth()
            .preemptive()
            .basic(CLIENT_ID, CLIENT_SECRET)
            .when()
            .post(BASE_URL + "/api/oauth2/token");
    if (response.statusCode() == 200) {
      return response.jsonPath().getString("access_token");
    }
    throw new SoftiqException("Failed to get access token: " + response.body().asString());
  }

  public String createPayment(
      String accessToken, String orderNumber, BigDecimal amount, String email, Boolean isNewUser) {
    return createGoPayPayment(accessToken, amount, "CZK", orderNumber, email, isNewUser);
  }

  private void savePaymentToDb(
      String state,
      BigDecimal amount,
      String currency,
      String orderNumber,
      String orderId,
      String email) {
    var client = watchDogManagementComponent.getCoreClient(email);
    var createdDateTime = LocalDateTime.now();
    var paymentEntity =
        new PaymentEntity(
            client,
            createdDateTime,
            createdDateTime,
            orderNumber,
            orderId,
            currency,
            amount,
            state,
            null);
    paymentRepository.save(paymentEntity);
  }

  public String generateOrderNumber() {
    List<String> latestOrders = paymentRepository.getLatestOrderNumber();
    String latestOrderNumber = latestOrders.isEmpty() ? "0" : latestOrders.getFirst();

    int nextOrderNumber = Integer.parseInt(latestOrderNumber) + 1;
    return String.valueOf(nextOrderNumber);
  }

  private String createGoPayPayment(
      String accessToken,
      BigDecimal amount,
      String currency,
      String orderNumber,
      String userEmail,
      Boolean isNewUser) {
    String returnUrl = "";
    if (isNewUser) {
      if (profileChecker.getActiveProfile().equals(DEV)) {
        returnUrl = "http://localhost:3000?registrace-platba";
      } else {
        returnUrl = "https://softiq.cz?registrace-platba";
      }

    } else {
      if (profileChecker.getActiveProfile().equals(DEV)) {
        returnUrl = "http://localhost:3000?zmena-clenstvi-platba";
      } else {
        returnUrl = "https://softiq.cz?zmena-clenstvi-platba";
      }
    }
    Map<String, Object> requestBody =
        Map.of(
            "amount",
            amount,
            "currency",
            currency,
            "order_number",
            orderNumber,
            "payer",
            Map.of("contact", Map.of("email", userEmail)),
            "recurrence",
            Map.of(
                "recurrence_cycle",
                "MONTH",
                "recurrence_period",
                1,
                "recurrence_date_to",
                "2099-12-31"),
            "target",
            Map.of("type", "ACCOUNT", "goid", GO_ID),
            "callback",
            Map.of(
                "return_url",
                returnUrl,
                "notification_url",
                "https://softiq.cz/api/v2/public/payment/check"));
    Response response =
        RestAssured.given()
            .baseUri(BASE_URL + "/api/payments/payment")
            .contentType(ContentType.JSON)
            .header("Authorization", "Bearer " + accessToken)
            .body(requestBody)
            .post();

    if (response.statusCode() == 200) {
      String state = response.jsonPath().getString("state");
      String orderId = response.jsonPath().getString("id");
      savePaymentToDb(state, amount, "CZK", orderNumber, orderId, userEmail);
      return response.jsonPath().getString("gw_url");
    } else {
      throw new SoftiqException("Failed to create payment: " + response.body().asString());
    }
  }

  public String getPaymentStatus(String accessToken, String id, Boolean isNewClient) {
    Response response =
        RestAssured.given()
            .baseUri(BASE_URL + "/api/payments/payment/" + id)
            .header("Authorization", "Bearer " + accessToken)
            .get();
    String message = "";
    if (response.statusCode() == 200) {
      String state = response.jsonPath().getString("state");
      var payment = paymentRepository.findByOrderId(id).getFirst();
      payment.setState(state);

      PaymentStateEnum paymentState = fromState(state);
      message =
          (paymentState == null)
              ? "Neznámý stav platby"
              : (paymentState == PAID)
                  ? (isNewClient ? "Registrace úspěšně dokončena" : "Změna členství úspěšná")
                  : paymentState.getMessage();

      if (payment.getNote() != null && payment.getNote().startsWith("Recurring payment parent: ")) {
        payment.setNote(payment.getNote().split(";", 2)[0] + "; " + message);
      } else {
        payment.setNote(message);
      }
      paymentRepository.save(payment);
      return state;
    }
    if (response.statusCode() == 409) {
      return ERROR_1.getMessage();
    }
    return ERROR_2.getMessage();
  }

  public PaymentEntity getRecurringPayment(String parentId, String id) {
    Response response =
        RestAssured.given()
            .baseUri(BASE_URL + "/api/payments/payment/" + id)
            .header("Authorization", "Bearer " + getAccessToken())
            .get();
    if (response.statusCode() != 200) {
      throw new SoftiqException("Failed to verify recurring payment " + id);
    }
    JsonPath details = response.jsonPath();
    if (!id.equals(details.getString("id")) || !parentId.equals(details.getString("parent_id"))) {
      throw new SoftiqException("Recurring payment does not match its parent");
    }
    var parents = paymentRepository.findByOrderId(parentId);
    if (parents.size() != 1) {
      throw new SoftiqException("Recurring payment parent not found or ambiguous");
    }
    var parent = parents.getFirst();
    String state = details.getString("state");
    BigDecimal amount = new BigDecimal(details.getString("amount"));
    String currency = details.getString("currency");
    if (amount.compareTo(parent.getAmount()) != 0 || !parent.getCurrency().equals(currency)) {
      throw new SoftiqException("Recurring payment amount or currency does not match");
    }
    return new PaymentEntity(
        parent.getClient(),
        LocalDateTime.now(),
        LocalDateTime.now(),
        details.getString("order_number"),
        id,
        currency,
        amount,
        state,
        "Recurring payment parent: " + parentId);
  }

  public Boolean verifyReccuringPaymentState(String accessToken, String id) {
    if (TEST_PAYMENT_IDS.contains(id)) {
      return true;
    }
    Response response =
        RestAssured.given()
            .baseUri(BASE_URL + "/api/payments/payment/" + id)
            .header("Authorization", "Bearer " + accessToken)
            .get();
    if (response.statusCode() == 200) {
      JsonPath jsonPath = response.jsonPath();
      Map<String, Object> recurrence = jsonPath.getJsonObject("recurrence");

      if (recurrence != null) {
        String recurrenceState = (String) recurrence.get("recurrence_state");

        if (STOPPED.getState().equalsIgnoreCase(recurrenceState)) {
          var payment = paymentRepository.findByOrderId(id).getFirst();
          if (payment != null) {
            payment.setState(STOPPED.getState());
            payment.setNote(STOPPED.getMessage());
            paymentRepository.save(payment);
          }
          return false;
        }
      }
      return true;
    }

    String error = "Failed to check payments recurrence_state " + id;
    CoreLogEntity logEntity = new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), error);
    saveLog(logEntity);
    log.error(error);
    return true;
  }

  public String stopRecurringPayment(
      String accessToken, CoreClientEntity coreClientEntity, Boolean doDeactivateAccount) {
    List<PaymentEntity> payments = paymentRepository.findByClient(coreClientEntity);
    if (payments.isEmpty()) {
      return NO_PAYMENT.getMessage();
    }

    var sizeOfPaymentHistory = payments.size();
    if (sizeOfPaymentHistory < 2 && !payments.getFirst().getState().equals(PAID.getState())) {
      return ERROR_4.getMessage();
    }

    if (sizeOfPaymentHistory < 2 && payments.getFirst().getState().equals(PAID.getState())) {
      return ERROR_6.getMessage();
    }

    var latestPayment = payments.get(sizeOfPaymentHistory - (doDeactivateAccount ? 1 : 2));
    var id = latestPayment.getOrderId();
    Response response =
        RestAssured.given()
            .baseUri(BASE_URL + "/api/payments/payment/" + id + "/void-recurrence")
            .header("Authorization", "Bearer " + accessToken)
            .post();
    String message = "";
    if (response.statusCode() != 200) {
      return ERROR_3.getMessage();
    }
    String state = response.jsonPath().getString("result");
    latestPayment.setState(state);

    PaymentStateEnum paymentState = fromState(state);
    message = (paymentState == null) ? "Neznámý stav platby" : paymentState.getMessage();

    latestPayment.setNote(message);
    paymentRepository.save(latestPayment);
    return state;
  }

  public CoreClientEntity getUserByPaymentOrderId(String id) {
    return paymentRepository.findByOrderId(id).getFirst().getClient();
  }

  public String getPaymentMessage(String id) {
    return paymentRepository.findByOrderId(id).getFirst().getNote();
  }

  public BigDecimal getAmountForOrderId(String id) {
    return paymentRepository.findByOrderId(id).getFirst().getAmount().divide(new BigDecimal(100));
  }

  public void handleBypassPaywall(String email) {
    var coreClient = watchDogManagementComponent.getCoreClient(email);
    var watchDogClient = watchDogManagementComponent.getWatchDogMembership(coreClient);
    if (watchDogClient == null) {
      watchDogClient = watchDogManagementComponent.getNewWatchDogClient(coreClient);
    }
    try {
      watchDogManagementComponent.enableAccount(email);
      watchDogManagementComponent.changeMembership(
          email, watchDogClient.getPendingMembership(), BigDecimal.ZERO, true);
    } catch (Exception e) {
      String errorMessage = "Chyba při registraci: " + e.getMessage();
      CoreLogEntity logEntity =
          new CoreLogEntity(null, LocalDate.now(), LocalDateTime.now(), errorMessage);
      saveLog(logEntity);
      log.error(errorMessage);
      throw new SoftiqException(errorMessage);
    }
  }

  public void softChangeMembership(
      String email, MembershipEnum membership, BigDecimal amount, PromoCodeEnum promoCode) {
    watchDogManagementComponent.softChangeMembershipProcess(email, membership, amount, promoCode);
  }

  public List<PaymentEntity> getAllActivePayments() {
    List<PaymentEntity> payments = new ArrayList<>();
    payments.addAll(paymentRepository.getAllByState(PAID.getState()));
    payments.addAll(paymentRepository.getAllByState(PaymentStateEnum.RECURRENCE_OK.getState()));
    payments.removeIf(payment -> TEST_PAYMENT_IDS.contains(payment.getOrderId()));
    return payments;
  }
}
