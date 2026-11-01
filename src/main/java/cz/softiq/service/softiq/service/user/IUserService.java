package cz.softiq.service.softiq.service.user;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.service.watchdog.entity.WatchDogMembershipEntity;
import java.util.List;
import java.util.Map;

public interface IUserService extends IBeanProvider, IBaseService {

  private UserService getUserService() {
    return getBean(UserService.class);
  }

  default Map<String, Object> login(String email) {
    return getUserService().loadUserInfo(email);
  }

  default List<WatchDogMembershipEntity> getAllActiveUsers() {
    return getUserService().getAllActiveUsers();
  }

  default Map<String, Object> register(String email, String password) {
    return getUserService().register(email, password);
  }
}
