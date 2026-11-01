package cz.softiq.service.softiq.model.builder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import cz.softiq.service.softiq.entity.BlogPostEntity;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BlogPost {

  private String id;
  private String url;
  private String title;

  @JsonProperty("content_html")
  private String contentHtml;

  @JsonProperty("key_takeaways")
  private String keyTakeaways;

  private String summary;

  private String image;

  private String author;

  private String language;

  @JsonProperty("publish_date")
  private Instant publishDate;

  @JsonProperty("date_modified")
  private OffsetDateTime dateModified;

  @JsonProperty("_seo")
  private Seo seo;

  public static BlogPost from(BlogPostEntity entity) {
    BlogPost post = new BlogPost();
    post.setId(
        entity.getLegacyId() == null ? String.valueOf(entity.getId()) : entity.getLegacyId());
    post.setUrl(entity.getSlug());
    post.setTitle(entity.getTitle());
    post.setContentHtml(entity.getContentHtml());
    post.setKeyTakeaways(entity.getKeyTakeaways());
    post.setSummary(entity.getSummary());
    post.setImage(entity.getImage());
    post.setAuthor(entity.getAuthor());
    post.setLanguage(entity.getLanguage());
    post.setPublishDate(entity.getPublishDate());
    post.setDateModified(
        entity.getUpdatedAt() == null
            ? null
            : entity.getUpdatedAt().atOffset(java.time.ZoneOffset.UTC));

    Seo postSeo = new Seo();
    postSeo.setMetaDescription(entity.getMetaDescription());
    postSeo.setMetaKeywords(entity.getMetaKeywords());
    post.setSeo(postSeo);
    return post;
  }

  @Data
  public static class Seo {

    @JsonProperty("meta_description")
    private String metaDescription;

    @JsonProperty("meta_keywords")
    private List<String> metaKeywords;
  }
}
