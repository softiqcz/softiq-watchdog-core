package cz.softiq.service.softiq.service.captcha;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;

public interface ICaptchaService extends IBeanProvider, ICoreSoftiqService, IBaseService {
  private ReCaptchaService getRecaptchaService() {
    return getBean(ReCaptchaService.class);
  }

  default boolean verifyRecaptcha(String recaptchaToken, String recaptchaSecret) {
    return getRecaptchaService().verifyRecaptcha(recaptchaToken, recaptchaSecret);
  }
}
