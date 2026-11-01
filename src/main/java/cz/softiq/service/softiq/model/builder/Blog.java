package cz.softiq.service.softiq.model.builder;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Blog {

  @JsonProperty("items")
  private List<BlogPost> blogPosts;

  private String author;
  private String language;
}
