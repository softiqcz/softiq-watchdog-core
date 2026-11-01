package cz.softiq.service.watchdog.repository;

import cz.softiq.common.base.model.IBaseRepository;
import cz.softiq.core.clients.CoreClientEntity;
import cz.softiq.service.watchdog.entity.PaymentEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer>, IBaseRepository {
  @Query("SELECT p.id FROM PaymentEntity p ORDER BY p.id DESC")
  List<String> getLatestOrderNumber();

  List<PaymentEntity> findByOrderId(String orderId);

  @Query(
      "SELECT p FROM PaymentEntity p WHERE p.client = :client "
          + "AND (p.note IS NULL OR p.note NOT LIKE 'Recurring payment parent: %') ORDER BY p.id")
  List<PaymentEntity> findByClient(CoreClientEntity client);

  @Query(
      "SELECT p FROM PaymentEntity p WHERE p.state = :state "
          + "AND (p.note IS NULL OR p.note NOT LIKE 'Recurring payment parent: %')")
  List<PaymentEntity> getAllByState(String state);

  PaymentEntity getByClient(CoreClientEntity client);
}
