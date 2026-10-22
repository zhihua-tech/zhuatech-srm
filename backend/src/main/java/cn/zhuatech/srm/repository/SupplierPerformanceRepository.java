package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierPerformance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupplierPerformanceRepository extends JpaRepository<SupplierPerformance, Long> {
    List<SupplierPerformance> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    List<SupplierPerformance> findBySupplierIdAndDeletedFalse(Long supplierId);
}
