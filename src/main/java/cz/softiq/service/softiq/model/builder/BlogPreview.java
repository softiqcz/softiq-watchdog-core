package cz.softiq.service.softiq.model.builder;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record BlogPreview(
    String author, String language, @JsonProperty("items") List<BlogPostPreview> blogPosts) {}
