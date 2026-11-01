package cz.softiq.service.realiq.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.realiq.model.entities.ClientEstate;
import cz.softiq.service.realiq.model.entities.Transaction;
import cz.softiq.service.realiq.model.enums.TransactionType;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long>, IBaseRepository {
  boolean existsByEstateClient(CoreClientEntity client);

  boolean existsByEstateIdAndBankImportId(Long estateId, String bankImportId);

  List<Transaction> findAllByEstateIdAndBankImportIdIn(Long estateId, Collection<String> importIds);

  List<Transaction> findAllByEstateIdAndBankImportSeriesIdOrderByDateFromDesc(
      Long estateId, String bankImportSeriesId);

  List<Transaction> findAllByEstateIdAndBankImportSeriesIdIn(
      Long estateId, Collection<String> bankImportSeriesIds);

  List<Transaction> findAllByEstateIdOrderByDateFromDesc(Long estateId);

  List<Transaction>
      findAllByEstateIdAndNameIgnoreCaseAndTransactionTypeAndBankImportIdIsNotNullOrderByDateFromDesc(
          Long estateId, String name, TransactionType transactionType);

  /**
   * Find transactions that should be counted in the given month period. - ONCE transactions: only
   * if date_from is within the period - YEARLY transactions: only if date_from month matches the
   * period month (anniversary logic) - MONTHLY transactions: if active during the period (started
   * before/during AND not ended or ends during/after)
   */
  @org.springframework.data.jpa.repository.Query(
      "SELECT t FROM Transaction t WHERE t.estate IN :estates "
          + "AND ("
          + "  (t.recurrence = 'ONCE' AND t.dateFrom >= :periodStart AND t.dateFrom <= :periodEnd) "
          + "  OR "
          + "  (t.recurrence = 'YEARLY' AND MONTH(t.dateFrom) = MONTH(:periodStart) AND t.dateFrom <= :periodEnd AND (t.dateTo IS NULL OR t.dateTo >= :periodStart)) "
          + "  OR "
          + "  (t.recurrence = 'MONTHLY' AND t.dateFrom <= :periodEnd AND (t.dateTo IS NULL OR t.dateTo >= :periodStart))"
          + ")")
  List<Transaction> findActiveTransactionsByEstatesAndPeriod(
      @org.springframework.data.repository.query.Param("estates") List<ClientEstate> estates,
      @org.springframework.data.repository.query.Param("periodStart") Date periodStart,
      @org.springframework.data.repository.query.Param("periodEnd") Date periodEnd);
}
