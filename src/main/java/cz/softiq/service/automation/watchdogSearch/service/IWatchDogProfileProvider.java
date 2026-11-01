package cz.softiq.service.automation.watchdogSearch.service;

import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.configuration.profile.Profile;
import cz.softiq.core.configuration.profile.ProfileChecker;

public interface IWatchDogProfileProvider extends IBeanProvider {

  default Profile getActiveProfile() {
    return getBean(ProfileChecker.class).getActiveProfile();
  }
}
