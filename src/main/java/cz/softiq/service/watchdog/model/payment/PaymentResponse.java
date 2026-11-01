package cz.softiq.service.watchdog.model.payment;

import lombok.Data;

@Data
public class PaymentResponse {
  private String message;
  private String status;
  private String error;

  public PaymentResponse(String message, String status, String error) {
    this.message = message;
    this.status = status;
    this.error = error;
  }
}
