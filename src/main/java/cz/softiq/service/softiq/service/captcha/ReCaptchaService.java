package cz.softiq.service.softiq.service.captcha;

import cz.softiq.common.base.model.IBaseService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ReCaptchaService implements IBaseService {
  private final RestTemplate restTemplate = new RestTemplate();

  boolean verifyRecaptcha(String recaptchaToken, String recaptchaSecret) {
    String verifyUrl =
        "https://www.google.com/recaptcha/api/siteverify?secret="
            + recaptchaSecret
            + "&response="
            + recaptchaToken;
    ResponseEntity<Map> response = restTemplate.postForEntity(verifyUrl, null, Map.class);
    Map<?, ?> body = response.getBody();
    boolean success = body != null && Boolean.TRUE.equals(body.get("success"));
    Object scoreObj = body != null ? body.get("score") : null;
    double score = scoreObj instanceof Number ? ((Number) scoreObj).doubleValue() : 0.0;
    return success && score >= 0.5;
  }
}
