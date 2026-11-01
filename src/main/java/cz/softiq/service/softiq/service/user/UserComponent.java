package cz.softiq.service.softiq.service.user;

import static cz.softiq.service.softiq.model.constants.MessageConstants.INVALID_CREDENTIALS;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.softiq.model.constants.MessageConstants;
import cz.softiq.service.softiq.service.preferences.IUserPreferencesService;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import cz.softiq.service.watchdog.model.enums.MembershipEnum;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

@Component
public class UserComponent implements IWatchDogAccountService, IUserPreferencesService {

  private Map<String, Object> getAllUserInfo(CoreClientEntity user) {
    try {
      Map<String, Object> response = new HashMap<>();
      response.put("email", user.getEmail());
      WatchDogMembershipEntity membership = getWatchDogMembershipByEmail(user.getEmail());
      response.put("membership", membership.getMembership().getName());
      response.put("agreedPrice", membership.getAgreedPrice().intValue());
      Integer maxWatchDogs =
          membership.getPromoCode() != null
                  && membership.getPromoCode().appliesTo(membership.getMembership())
                  && membership.getPromoCode().getMaxWatchdogs() != null
              ? membership.getPromoCode().getMaxWatchdogs()
              : membership.getMembership().getMaxCountOfWatchdogs();
      response.put(
          "membershipInfo",
          membership.getPromoCode() != null
              ? maxWatchDogs + " hlídacích psů"
              : membership.getMembershipInfo());
      response.put("maxWatchDogs", maxWatchDogs);
      response.put("promoCode", membership.getPromoCode());
      if (membership.getPromoCode() != null) {
        response.put(
            "promoDiscountPercent",
            BigDecimal.ONE
                .subtract(membership.getPromoCode().getMultiplier())
                .multiply(BigDecimal.valueOf(100))
                .stripTrailingZeros());
        response.put(
            "promoFree", membership.getPromoCode().getMultiplier().compareTo(BigDecimal.ZERO) == 0);
        response.put("promoLifetime", membership.getPromoCode().isLifetime());
      }
      response.put("validToFormatted", membership.getValidToFormatted());
      response.put("validTo", membership.getValidTo());
      response.put("isActive", membership.getIsActive());
      response.put("isEligibleForFreeTier", membership.getMembership().getName());
      response.put("role", user.getRole());
      response.put("userName", membership.getUserName());
      response.put("idNumber", membership.getIdNumber());
      response.put("address", membership.getAddress());
      response.put("subject", membership.getSubject());
      response.put("preferences", getOrCreatePreferences(user));
      return response;
    } catch (Exception e) {
      throw new UsernameNotFoundException(INVALID_CREDENTIALS);
    }
  }

  public Map<String, Object> loadUserData(String email) {
    CoreClientEntity user = getCoreClientByEmail(email);
    return getAllUserInfo(user);
  }

  public List<WatchDogMembershipEntity> getAllActiveUsers() {
    return getActiveWatchDogMemberships();
  }

  public Map<String, Object> getRegisterResponse(
      String errorMessage, String email, String password) {
    Map<String, Object> userResponse = new HashMap<>();
    userResponse.put(MessageConstants.errorMessage, errorMessage);
    CoreClientEntity user = getCoreClientByEmail(email);
    if (user != null && BCrypt.checkpw(password, user.getPassword())) {
      userResponse.put("email", user.getEmail());
      userResponse.put("membership", MembershipEnum.REGISTER.getName());
      userResponse.put("isEligibleForFreeTier", isNewAccountEligibleForFreeMembership(user));
      return userResponse;
    }
    return userResponse;
  }
}
