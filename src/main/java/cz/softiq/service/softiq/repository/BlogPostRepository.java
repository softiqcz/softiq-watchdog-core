package cz.softiq.service.softiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.softiq.entity.BlogPostEntity;
import cz.softiq.service.softiq.model.enums.BlogStatus;
import cz.softiq.service.softiq.model.projection.BlogPostPreviewProjection;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BlogPostRepository extends JpaRepository<BlogPostEntity, Long>, IBaseRepository {

  List<BlogPostPreviewProjection> findAllProjectedByStatusOrderByPublishDateDesc(BlogStatus status);

  List<BlogPostPreviewProjection> findTop5ProjectedByStatusOrderByPublishDateDesc(
      BlogStatus status);

  List<BlogPostEntity> findAllByOrderByPublishDateDesc();

  Optional<BlogPostEntity> findBySlugAndStatus(String slug, BlogStatus status);

  boolean existsBySlug(String slug);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query(
      "update BlogPostEntity blog "
          + "set blog.status = :published, blog.publishedAt = :now, blog.updatedAt = :now "
          + "where blog.status = :scheduled and blog.publishDate <= :now")
  int publishDue(
      @Param("scheduled") BlogStatus scheduled,
      @Param("published") BlogStatus published,
      @Param("now") Instant now);
}
