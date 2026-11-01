package cz.softiq.service.softiq.model.builder;

import cz.softiq.core.security.RoleEnum;
import java.time.LocalDateTime;
import lombok.Data;
import org.apache.http.annotation.Obsolete;

@Data
@Obsolete // todo delete i guess
public class LoginResponse {
  private String token;
  private String email;
  private String membership;
  private String membershipInfo;
  private Integer agreedPrice;
  private String promoCode;
  private String validToFormatted;
  private LocalDateTime validTo;
  private Boolean isActive;
  private Boolean isEligibleForFreeTier;
  private RoleEnum role;
  private String userName;
  // BILLING INFO
  private String idNumber;
  private String address;
  private String subject;
  // PAYMENT
  private String gw_url;
  private String successMessage;
  private String errorMessage;
}
