package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SourcingClarification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SourcingClarificationRepository extends JpaRepository<SourcingClarification, Long> {
    List<SourcingClarification> findByEventIdAndSupplierIdAndDeletedFalseOrderByCreatedAt(Long eventId, Long supplierId);
}
