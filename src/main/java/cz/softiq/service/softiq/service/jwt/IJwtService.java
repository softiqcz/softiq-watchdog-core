package cz.softiq.service.softiq.service.jwt;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.core.configuration.IBeanProvider;
import org.springframework.security.core.userdetails.UserDetails;

public interface IJwtService extends IBeanProvider, IBaseService {

  private JwtService getJwtService() {
    return getBean(JwtService.class);
  }

  default String generateToken(UserDetails userDetails) {
    return getJwtService().generateToken(userDetails);
  }

  default String generateUnsubscribeToken(UserDetails userDetails) {
    return getJwtService().generateUnsubscribeToken(userDetails);
  }

  default String extractUsername(String token) {
    return getJwtService().extractUsername(token);
  }

  default CoreClientEntity getCurrentUser(String token) {
    return getJwtService().getCurrentUser(token);
  }

  default boolean isTokenValid(String token, UserDetails userDetails) {
    return getJwtService().isTokenValid(token, userDetails);
  }
}
