package com.taja.crm.crm_backend.repo;
import com.taja.crm.crm_backend.model.SalesOrder;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
public interface SalesOrderRepository extends JpaRepository<SalesOrder, String>, JpaSpecificationExecutor<SalesOrder> {
 @Lock(LockModeType.PESSIMISTIC_WRITE)
 @Query("select o from SalesOrder o where o.id = :id")
 Optional<SalesOrder> findForUpdateById(String id);
}
