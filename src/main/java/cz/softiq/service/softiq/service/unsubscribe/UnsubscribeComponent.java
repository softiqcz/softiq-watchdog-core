package cz.softiq.service.softiq.service.unsubscribe;

import cz.softiq.service.softiq.entity.UnsubscribeEntity;
import cz.softiq.service.softiq.repository.UnsubscribeRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class UnsubscribeComponent {
  private final UnsubscribeRepository unsubscribeRepository;

  public boolean existsByEmail(String email) {
    return unsubscribeRepository.existsByClientEmail(email);
  }

  public void saveIfMissing(String email) {
    unsubscribeRepository
        .findByClientEmail(email)
        .orElseGet(
            () -> unsubscribeRepository.save(new UnsubscribeEntity(email, LocalDateTime.now())));
  }
}
