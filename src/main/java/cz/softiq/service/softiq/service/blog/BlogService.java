package cz.softiq.service.softiq.service.blog;

import cz.softiq.service.softiq.entity.BlogPostEntity;
import cz.softiq.service.softiq.model.builder.BlogPost;
import cz.softiq.service.softiq.model.builder.BlogPreview;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class BlogService {
  private final BlogComponent blogComponent;

  BlogPreview loadPublishedBlogPreview() {
    return blogComponent.loadPublishedBlogPreview();
  }

  BlogPreview loadlatestPublishedBlogPreview() {
    return blogComponent.loadLatestPublishedBlogPreview();
  }

  BlogPost loadPublishedBlogPost(String slug) {
    return blogComponent.loadPublishedBlogPost(slug);
  }

  List<BlogPostEntity> loadAllBlogPosts() {
    return blogComponent.loadAllBlogPosts();
  }

  Map<String, Object> doBlogAction(Map<String, Object> request) {
    return blogComponent.doBlogAction(request);
  }

  List<BlogPostEntity> saveBlogPosts(Map<String, Object> request) {
    return blogComponent.saveBlogPosts(request);
  }

  void publishDueBlogPosts() {
    blogComponent.publishDueBlogPosts();
  }

  int importBundledBlogIfEmpty() {
    return blogComponent.importBundledBlogIfEmpty();
  }
}
