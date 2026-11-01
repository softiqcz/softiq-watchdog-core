package cz.softiq.service.softiq.service.blog;

import cz.softiq.common.base.model.IBaseService;
import cz.softiq.core.configuration.IBeanProvider;
import cz.softiq.core.service.ICoreSoftiqService;
import cz.softiq.service.softiq.entity.BlogPostEntity;
import cz.softiq.service.softiq.model.builder.BlogPost;
import cz.softiq.service.softiq.model.builder.BlogPreview;
import java.util.List;
import java.util.Map;

public interface IBlogService extends IBeanProvider, ICoreSoftiqService, IBaseService {
  private BlogService getBlogService() {
    return getBean(BlogService.class);
  }

  default BlogPreview loadPublishedBlogPreview() {
    return getBlogService().loadPublishedBlogPreview();
  }

  default BlogPreview loadlatestPublishedBlogPreview() {
    return getBlogService().loadlatestPublishedBlogPreview();
  }

  default BlogPost loadPublishedBlogPost(String slug) {
    return getBlogService().loadPublishedBlogPost(slug);
  }

  default List<BlogPostEntity> loadAllBlogPosts() {
    return getBlogService().loadAllBlogPosts();
  }

  default Map<String, Object> doBlogAction(Map<String, Object> request) {
    return getBlogService().doBlogAction(request);
  }

  default List<BlogPostEntity> saveBlogPosts(Map<String, Object> request) {
    return getBlogService().saveBlogPosts(request);
  }

  default void publishDueBlogPosts() {
    getBlogService().publishDueBlogPosts();
  }

  default int importBundledBlogIfEmpty() {
    return getBlogService().importBundledBlogIfEmpty();
  }
}
