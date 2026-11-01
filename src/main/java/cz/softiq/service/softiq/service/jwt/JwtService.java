package cz.softiq.service.softiq.service.jwt;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.softiq.service.user.IUserService;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class JwtService implements IUserService, IWatchDogAccountService {
  private final JwtComponent jwtComponent;

  public String generateToken(UserDetails userDetails) {
    List<String> roles =
        userDetails.getAuthorities().stream()
            .map(grantedAuthority -> grantedAuthority.getAuthority())
            .toList();

    return Jwts.builder()
        .subject(userDetails.getUsername())
        .claim("roles", roles)
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 10))
        .signWith(jwtComponent.getSignInKey())
        .compact();
  }

  public String extractUsername(String token) {
    return jwtComponent.extractAllClaims(token).getSubject();
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return (username.equals(userDetails.getUsername()) && !jwtComponent.isTokenExpired(token));
  }

  public CoreClientEntity getCurrentUser(String token) {
    String requestorEmail = extractUsername(token);
    return getCoreClientByEmail(requestorEmail);
  }

  public String generateUnsubscribeToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUsername())
        .expiration(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)))
        .signWith(jwtComponent.getSignInKey())
        .compact();
  }
}
