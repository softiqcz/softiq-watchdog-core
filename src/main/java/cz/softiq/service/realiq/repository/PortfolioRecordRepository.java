package cz.softiq.service.realiq.repository;

import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.PortfolioRecordEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PortfolioRecordRepository extends JpaRepository<PortfolioRecordEntity, String> {
  @Query(
      value =
          """
          SELECT CAST(COALESCE(SUM(JSON_LENGTH(documents)), 0) AS SIGNED)
          FROM portfolio_record
          WHERE client_name = :clientName
          """,
      nativeQuery = true)
  long countDocumentsByClientName(@Param("clientName") String clientName);

  List<PortfolioRecordEntity> findAllByClientAndEstate(
      CoreClientEntity client, ClientEstate estate);

  Optional<PortfolioRecordEntity> findByIdAndClient(String id, CoreClientEntity client);

  List<PortfolioRecordEntity> findAllByReminderAmountIsNotNullAndImportantDateIsNotNull();
}
