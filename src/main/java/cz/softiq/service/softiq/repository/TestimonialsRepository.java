package cz.softiq.service.softiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.service.softiq.entity.TestimonialsEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestimonialsRepository
    extends JpaRepository<TestimonialsEntity, Integer>, IBaseRepository {

  List<TestimonialsEntity> findAllByIsPublishedTrueOrderByCreatedAtDesc();
}
