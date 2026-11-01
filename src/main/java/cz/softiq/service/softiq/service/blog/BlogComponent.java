package cz.softiq.service.softiq.service.blog;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.softiq.service.realiq.model.enums.ActionEnum;
import cz.softiq.service.softiq.entity.BlogPostEntity;
import cz.softiq.service.softiq.model.builder.Blog;
import cz.softiq.service.softiq.model.builder.BlogPost;
import cz.softiq.service.softiq.model.builder.BlogPostPreview;
import cz.softiq.service.softiq.model.builder.BlogPostRequest;
import cz.softiq.service.softiq.model.builder.BlogPostUpdateRequest;
import cz.softiq.service.softiq.model.builder.BlogPreview;
import cz.softiq.service.softiq.model.enums.BlogStatus;
import cz.softiq.service.softiq.repository.BlogPostRepository;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
class BlogComponent {
  private static final String DEFAULT_AUTHOR = "Ing. Martin Švejda";
  private static final String DEFAULT_LANGUAGE = "cs";

  private final BlogPostRepository blogPostRepository;
  private final ObjectMapper objectMapper;

  public BlogPreview loadPublishedBlogPreview() {
    List<BlogPostPreview> posts =
        blogPostRepository
            .findAllProjectedByStatusOrderByPublishDateDesc(BlogStatus.PUBLISHED)
            .stream()
            .map(BlogPostPreview::from)
            .toList();
    return new BlogPreview(DEFAULT_AUTHOR, DEFAULT_LANGUAGE, posts);
  }

  public BlogPreview loadLatestPublishedBlogPreview() {
    List<BlogPostPreview> posts =
        blogPostRepository
            .findTop5ProjectedByStatusOrderByPublishDateDesc(BlogStatus.PUBLISHED)
            .stream()
            .map(BlogPostPreview::from)
            .toList();

    return new BlogPreview(DEFAULT_AUTHOR, DEFAULT_LANGUAGE, posts);
  }

  public BlogPost loadPublishedBlogPost(String slug) {
    return blogPostRepository
        .findBySlugAndStatus(slug, BlogStatus.PUBLISHED)
        .map(BlogPost::from)
        .orElse(null);
  }

  public List<BlogPostEntity> loadAllBlogPosts() {
    return blogPostRepository.findAllByOrderByPublishDateDesc();
  }

  @Transactional
  public Map<String, Object> doBlogAction(Map<String, Object> request) {
    Map<String, Object> data = BlogPostRequest.requestData(request);
    ActionEnum action = parseAction(data);
    return switch (action) {
      case ADD -> actionResult(action, create(BlogPostRequest.from(data)), null);
      case EDIT -> actionResult(action, editBlogPost(data, requiredId(data)), null);
      case DELETE -> {
        Long id = requiredId(data);
        removeBlogPost(id);
        yield actionResult(action, null, id);
      }
    };
  }

  @Transactional
  public List<BlogPostEntity> saveBlogPosts(Map<String, Object> request) {
    List<BlogPostRequest> posts =
        BlogPostRequest.requestItems(request).stream().map(BlogPostRequest::from).toList();
    validateUniqueSlugs(posts);
    return posts.stream().map(this::create).toList();
  }

  private BlogPostEntity editBlogPost(Map<String, Object> data, Long id) {
    BlogPostEntity entity = findBlogPost(id);
    BlogPostUpdateRequest update = BlogPostUpdateRequest.from(data);
    applyUpdate(entity, update);
    return blogPostRepository.save(entity);
  }

  private void removeBlogPost(Long id) {
    findBlogPost(id);
    blogPostRepository.deleteById(id);
  }

  @Transactional
  public int publishDueBlogPosts() {
    Instant now = Instant.now();
    int published = blogPostRepository.publishDue(BlogStatus.SCHEDULED, BlogStatus.PUBLISHED, now);
    log.info("Published {} due blog posts at {}", published, now);
    return published;
  }

  @Transactional
  public int importBundledBlogIfEmpty() {
    if (blogPostRepository.count() > 0) {
      return 0;
    }

    Blog bundledBlog = readBundledBlog();
    List<BlogPostEntity> posts =
        bundledBlog.getBlogPosts().stream().map(post -> legacyEntity(post, bundledBlog)).toList();
    blogPostRepository.saveAll(posts);
    log.info("Imported {} bundled blog posts", posts.size());
    return posts.size();
  }

  private BlogPostEntity create(BlogPostRequest request) {
    if (blogPostRepository.existsBySlug(request.slug())) {
      throw new IllegalArgumentException(
          "A blog post with this slug already exists: " + request.slug());
    }
    return blogPostRepository.save(newEntity(request));
  }

  private BlogPostEntity findBlogPost(Long id) {
    return blogPostRepository
        .findById(id)
        .orElseThrow(() -> new NoSuchElementException("Blog post not found: " + id));
  }

  private ActionEnum parseAction(Map<String, Object> data) {
    Object value = data.containsKey("action") ? data.get("action") : data.get("type");
    if (value == null || value.toString().isBlank()) {
      throw new IllegalArgumentException("Missing required field: action");
    }
    try {
      return ActionEnum.valueOf(value.toString().trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("action must be ADD, EDIT, or DELETE.");
    }
  }

  private Long requiredId(Map<String, Object> data) {
    Object value = data.get("id");
    if (value == null) {
      throw new IllegalArgumentException("Missing required field: id");
    }
    try {
      long id =
          value instanceof Number number ? number.longValue() : Long.parseLong(value.toString());
      if (id <= 0) {
        throw new NumberFormatException();
      }
      return id;
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("id must be a positive number.");
    }
  }

  private Map<String, Object> actionResult(
      ActionEnum action, BlogPostEntity blogPost, Long deletedId) {
    Map<String, Object> result = new LinkedHashMap<>();
    result.put("action", action);
    if (blogPost != null) {
      result.put("blogPost", blogPost);
    }
    if (deletedId != null) {
      result.put("deletedId", deletedId);
    }
    return result;
  }

  private BlogPostEntity newEntity(BlogPostRequest request) {
    BlogPostEntity entity = new BlogPostEntity();
    entity.setSlug(request.slug());
    entity.setTitle(request.title());
    entity.setContentHtml(request.contentHtml());
    entity.setKeyTakeaways(request.keyTakeaways());
    entity.setSummary(request.summary());
    entity.setImage(request.image());
    entity.setAuthor(defaultText(request.author(), DEFAULT_AUTHOR));
    entity.setLanguage(defaultText(request.language(), DEFAULT_LANGUAGE));
    entity.setMetaDescription(request.metaDescription());
    entity.setMetaKeywords(request.metaKeywords());
    entity.setPublishDate(request.publishDate());
    applyStatus(entity, request.status());
    return entity;
  }

  private void applyUpdate(BlogPostEntity entity, BlogPostUpdateRequest update) {
    if (update.slug() != null && !update.slug().equals(entity.getSlug())) {
      if (blogPostRepository.existsBySlug(update.slug())) {
        throw new IllegalArgumentException(
            "A blog post with this slug already exists: " + update.slug());
      }
      entity.setSlug(update.slug());
    }
    setIfPresent(update.title(), entity::setTitle);
    setIfPresent(update.contentHtml(), entity::setContentHtml);
    setIfPresent(update.keyTakeaways(), entity::setKeyTakeaways);
    setIfPresent(update.summary(), entity::setSummary);
    setIfPresent(update.image(), entity::setImage);
    setIfPresent(update.author(), entity::setAuthor);
    setIfPresent(update.language(), entity::setLanguage);
    setIfPresent(update.metaDescription(), entity::setMetaDescription);
    setIfPresent(update.metaKeywords(), entity::setMetaKeywords);
    setIfPresent(update.publishDate(), entity::setPublishDate);
    if (update.status() != null) {
      applyStatus(entity, update.status());
    }
  }

  private void applyStatus(BlogPostEntity entity, BlogStatus status) {
    entity.setStatus(status);
    if (status == BlogStatus.PUBLISHED && entity.getPublishedAt() == null) {
      entity.setPublishedAt(Instant.now());
    } else if (status != BlogStatus.PUBLISHED) {
      entity.setPublishedAt(null);
    }
  }

  private void validateUniqueSlugs(List<BlogPostRequest> posts) {
    long uniqueSlugCount = posts.stream().map(BlogPostRequest::slug).distinct().count();
    if (uniqueSlugCount != posts.size()) {
      throw new IllegalArgumentException(
          "Every blog post in a bulk request must have a unique slug.");
    }
  }

  private Blog readBundledBlog() {
    try (InputStream input = new ClassPathResource("json/blog.json").getInputStream()) {
      return objectMapper.readValue(input, Blog.class);
    } catch (IOException e) {
      throw new IllegalStateException("Could not import the bundled blog file.", e);
    }
  }

  private BlogPostEntity legacyEntity(BlogPost post, Blog blog) {
    BlogPostEntity entity = new BlogPostEntity();
    entity.setLegacyId(post.getId());
    entity.setSlug(post.getUrl());
    entity.setTitle(post.getTitle());
    entity.setContentHtml(post.getContentHtml());
    entity.setKeyTakeaways(post.getKeyTakeaways());
    entity.setSummary(post.getSummary());
    entity.setImage(post.getImage());
    entity.setAuthor(defaultText(post.getAuthor(), blog.getAuthor()));
    entity.setLanguage(defaultText(post.getLanguage(), blog.getLanguage()));
    if (post.getSeo() != null) {
      entity.setMetaDescription(post.getSeo().getMetaDescription());
      entity.setMetaKeywords(post.getSeo().getMetaKeywords());
    }
    Instant publishDate = post.getPublishDate();
    if (publishDate == null && post.getDateModified() != null) {
      publishDate = post.getDateModified().toInstant();
    }
    entity.setPublishDate(publishDate);
    entity.setStatus(BlogStatus.PUBLISHED);
    entity.setPublishedAt(publishDate);
    return entity;
  }

  private String defaultText(String value, String defaultValue) {
    return value == null || value.isBlank() ? defaultValue : value;
  }

  private <T> void setIfPresent(T value, java.util.function.Consumer<T> setter) {
    if (value != null) {
      setter.accept(value);
    }
  }
}
