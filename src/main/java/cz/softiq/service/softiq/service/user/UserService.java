package cz.softiq.service.softiq.service.user;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.service.automation.watchdogSearch.service.IWatchDogAccountService;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService implements IWatchDogAccountService, IBaseService {

  private final UserComponent userComponent;

  Map<String, Object> loadUserInfo(String email) {
    return userComponent.loadUserData(email);
  }

  List<WatchDogMembershipEntity> getAllActiveUsers() {
    return userComponent.getAllActiveUsers();
  }

  Map<String, Object> register(String email, String password) {
    String errorMessage = createNewUserFromRegistration("recaptchaToken", email, password);
    return userComponent.getRegisterResponse(errorMessage, email, password);
  }
}
