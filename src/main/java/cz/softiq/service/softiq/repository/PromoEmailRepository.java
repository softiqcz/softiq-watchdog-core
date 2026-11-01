package cz.softiq.service.softiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.softiq.entity.PromoEmailEntity;
import cz.softiq.service.softiq.model.enums.PromoEmailType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PromoEmailRepository
    extends JpaRepository<PromoEmailEntity, Long>, IBaseRepository {
  boolean existsByClientAndSentAtAfter(CoreClientEntity client, LocalDateTime sentAt);

  boolean existsByClientAndEmailTypeAndSentAtGreaterThanEqual(
      CoreClientEntity client, PromoEmailType emailType, LocalDateTime sentAt);
}
