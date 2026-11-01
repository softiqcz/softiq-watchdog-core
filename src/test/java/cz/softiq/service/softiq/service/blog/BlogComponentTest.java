package cz.softiq.service.softiq.service.blog;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.softiq.service.softiq.entity.BlogPostEntity;
import cz.softiq.service.softiq.model.enums.BlogStatus;
import cz.softiq.service.softiq.model.projection.BlogPostPreviewProjection;
import cz.softiq.service.softiq.repository.BlogPostRepository;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class BlogComponentTest {

  @Test
  public void createBlogPostKeepsFuturePostScheduled() {
    BlogPostRepository repository = mock(BlogPostRepository.class);
    when(repository.save(any(BlogPostEntity.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    BlogComponent component = new BlogComponent(repository, new ObjectMapper());

    Map<String, Object> result = component.doBlogAction(singleRequest("2026-09-08T06:00:00+02:00"));
    BlogPostEntity saved = (BlogPostEntity) result.get("blogPost");

    assertEquals(result.get("action").toString(), "ADD");
    assertEquals(saved.getStatus(), BlogStatus.SCHEDULED);
    assertEquals(saved.getPublishDate(), Instant.parse("2026-09-08T04:00:00Z"));
    assertNull(saved.getPublishedAt());
  }

  @Test
  public void publishDueBlogPostsUsesInclusiveRepositoryUpdate() {
    BlogPostRepository repository = mock(BlogPostRepository.class);
    when(repository.publishDue(
            eq(BlogStatus.SCHEDULED), eq(BlogStatus.PUBLISHED), any(Instant.class)))
        .thenReturn(3);
    BlogComponent component = new BlogComponent(repository, new ObjectMapper());

    Instant before = Instant.now();
    int published = component.publishDueBlogPosts();
    Instant after = Instant.now();

    ArgumentCaptor<Instant> now = ArgumentCaptor.forClass(Instant.class);
    verify(repository)
        .publishDue(eq(BlogStatus.SCHEDULED), eq(BlogStatus.PUBLISHED), now.capture());
    assertEquals(published, 3);
    assertFalse(now.getValue().isBefore(before));
    assertFalse(now.getValue().isAfter(after));
  }

  @Test
  public void publicBlogLoadsOnlyPublishedPosts() {
    BlogPostRepository repository = mock(BlogPostRepository.class);
    BlogPostPreviewProjection projection = mock(BlogPostPreviewProjection.class);
    when(projection.getId()).thenReturn(42L);
    when(projection.getSlug()).thenReturn("preview-post");
    when(projection.getTitle()).thenReturn("Preview title");
    when(repository.findAllProjectedByStatusOrderByPublishDateDesc(BlogStatus.PUBLISHED))
        .thenReturn(java.util.List.of(projection));
    BlogComponent component = new BlogComponent(repository, new ObjectMapper());

    var preview = component.loadPublishedBlogPreview().blogPosts().getFirst();
    assertEquals(preview.id(), "42");
    assertEquals(preview.slug(), "preview-post");
    assertEquals(preview.title(), "Preview title");
    verify(repository).findAllProjectedByStatusOrderByPublishDateDesc(BlogStatus.PUBLISHED);
  }

  @Test
  public void editActionUpdatesTheRequestedBlogPost() {
    BlogPostRepository repository = mock(BlogPostRepository.class);
    BlogPostEntity existing = new BlogPostEntity();
    existing.setId(42L);
    existing.setSlug("scheduled-article");
    existing.setTitle("Old title");
    when(repository.findById(42L)).thenReturn(Optional.of(existing));
    when(repository.save(existing)).thenReturn(existing);
    BlogComponent component = new BlogComponent(repository, new ObjectMapper());

    Map<String, Object> result =
        component.doBlogAction(
            Map.of("data", Map.of("action", "EDIT", "id", 42, "title", "New title")));

    assertEquals(result.get("action").toString(), "EDIT");
    assertSame(result.get("blogPost"), existing);
    assertEquals(existing.getTitle(), "New title");
    verify(repository).save(existing);
  }

  @Test
  public void deleteActionDeletesTheRequestedBlogPost() {
    BlogPostRepository repository = mock(BlogPostRepository.class);
    BlogPostEntity existing = new BlogPostEntity();
    existing.setId(42L);
    when(repository.findById(42L)).thenReturn(Optional.of(existing));
    BlogComponent component = new BlogComponent(repository, new ObjectMapper());

    Map<String, Object> result =
        component.doBlogAction(Map.of("data", Map.of("action", "DELETE", "id", 42)));

    assertEquals(result.get("action").toString(), "DELETE");
    assertEquals(result.get("deletedId"), 42L);
    verify(repository).deleteById(42L);
  }

  private Map<String, Object> singleRequest(String publishDate) {
    return Map.of(
        "data",
        Map.of(
            "action", "ADD",
            "slug", "scheduled-article",
            "title", "Scheduled article",
            "contentHtml", "<p>Body</p>",
            "publishDate", publishDate));
  }
}
