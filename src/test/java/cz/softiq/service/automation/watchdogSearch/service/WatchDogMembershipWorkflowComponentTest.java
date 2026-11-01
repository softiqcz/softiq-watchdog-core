package cz.softiq.service.automation.watchdogSearch.service;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.clients.CoreClientRepository;
import cz.softiq.core.configuration.SoftiqException;
import cz.softiq.service.watchdog.component.WatchDogManagementComponent;
import cz.softiq.service.watchdog.entity.PaymentEntity;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import cz.softiq.service.watchdog.repository.PaymentRepository;
import cz.softiq.service.watchdog.repository.WatchDogMembershipRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.testng.annotations.Test;

public class WatchDogMembershipWorkflowComponentTest {
  private static class Fixture extends WatchDogMembershipWorkflowComponent {
    private final PaymentEntity payment = new PaymentEntity();
    private int invoices;
    private boolean verificationFails;

    Fixture(PaymentRepository payments, WatchDogMembershipRepository memberships) {
      super(
          mock(WatchDogManagementComponent.class),
          mock(CoreClientRepository.class),
          payments,
          memberships);
      payment.setOrderId("child");
      payment.setAmount(new java.math.BigDecimal("100"));
    }

    @Override
    public PaymentEntity getRecurringPayment(String parentId, String id) {
      if (verificationFails) throw new SoftiqException("Gateway unavailable");
      return payment;
    }

    @Override
    public void prolongUser(
        String email, MembershipEnum membership, String price, WatchDogMembershipEntity entity) {
      invoices++;
    }
  }

  @Test
  public void unpaidCallbacksNeverExtendMembershipOrIssueInvoices() {
    for (String state :
        List.of("CANCELED", "CREATED", "TIMEOUTED", "PAYMENT_METHOD_CHOSEN", "REFUNDED")) {
      PaymentRepository payments = mock(PaymentRepository.class);
      WatchDogMembershipRepository memberships = mock(WatchDogMembershipRepository.class);
      Fixture component = new Fixture(payments, memberships);
      component.payment.setState(state);
      when(payments.findByOrderId("child")).thenReturn(List.of());
      assertFalse(component.prolongRecurringMembership("parent", "child"));
      assertEquals(component.invoices, 0);
      verifyNoInteractions(memberships);
      verify(payments).save(component.payment);
      verify(payments, never()).findByOrderId("parent");
    }
  }

  @Test
  public void paidCallbackIssuesOneInvoiceAndDuplicateDoesNothing() {
    PaymentRepository payments = mock(PaymentRepository.class);
    WatchDogMembershipRepository memberships = mock(WatchDogMembershipRepository.class);
    Fixture component = new Fixture(payments, memberships);
    component.payment.setState("PAID");
    PaymentEntity parent = new PaymentEntity();
    CoreClientEntity client = mock(CoreClientEntity.class);
    parent.setClient(client);
    parent.setOrderNumber("198");
    WatchDogMembershipEntity membership = new WatchDogMembershipEntity();
    membership.setValidTo(LocalDateTime.now().minusDays(1));
    when(payments.findByOrderId("child"))
        .thenReturn(List.of())
        .thenReturn(List.of(component.payment));
    when(payments.findByOrderId("parent")).thenReturn(List.of(parent));
    when(memberships.findByIsActiveAndClient(true, client)).thenReturn(List.of(membership));
    assertTrue(component.prolongRecurringMembership("parent", "child"));
    LocalDateTime validTo = membership.getValidTo();
    assertTrue(component.prolongRecurringMembership("parent", "child"));
    assertEquals(component.invoices, 1);
    assertEquals(membership.getValidTo(), validTo);
    verify(memberships, times(1)).save(membership);
  }

  @Test
  public void gatewayFailureDoesNotChangeDatabaseOrIssueInvoices() {
    PaymentRepository payments = mock(PaymentRepository.class);
    WatchDogMembershipRepository memberships = mock(WatchDogMembershipRepository.class);
    Fixture component = new Fixture(payments, memberships);
    component.verificationFails = true;
    expectThrows(
        SoftiqException.class, () -> component.prolongRecurringMembership("parent", "child"));
    assertEquals(component.invoices, 0);
    verifyNoInteractions(payments, memberships);
  }
}
