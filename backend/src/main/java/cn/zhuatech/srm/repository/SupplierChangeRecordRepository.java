package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierChangeRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupplierChangeRecordRepository extends JpaRepository<SupplierChangeRecord, Long> {
    List<SupplierChangeRecord> findBySupplierIdAndDeletedFalseOrderByCreatedAtDesc(Long supplierId);
}
