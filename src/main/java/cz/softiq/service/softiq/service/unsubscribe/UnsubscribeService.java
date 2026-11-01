package cz.softiq.service.softiq.service.unsubscribe;

import static cz.softiq.service.softiq.model.enums.ContextType.*;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.service.softiq.service.jwt.IJwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UnsubscribeService implements IBaseService, IJwtService {
  private final UnsubscribeComponent unsubscribeComponent;

  String generateUnsubscribeLink(String email) {
    UserDetails user =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("")
            .authorities("ROLE_USER")
            .build();

    String token = generateToken(user);
    return "https://softiq.cz/?unsubscribe_token=" + token;
  }

  boolean canSendEmail(String email) {
    return !unsubscribeComponent.existsByEmail(email);
  }

  void unsubscribeUserByEmail(String email) {
    unsubscribeComponent.saveIfMissing(email);
  }
}
