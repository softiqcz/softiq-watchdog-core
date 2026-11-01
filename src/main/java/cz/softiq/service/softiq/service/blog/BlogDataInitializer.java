package cz.softiq.service.softiq.service.blog;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
class BlogDataInitializer implements ApplicationRunner, IBlogService {

  @Override
  public void run(ApplicationArguments args) {
    importBundledBlogIfEmpty();
  }
}
