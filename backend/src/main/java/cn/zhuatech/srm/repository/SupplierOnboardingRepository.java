package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierOnboardingApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SupplierOnboardingRepository extends JpaRepository<SupplierOnboardingApplication, Long> {
    Optional<SupplierOnboardingApplication> findFirstBySupplierIdAndDeletedFalseOrderByCreatedAtDesc(Long supplierId);
    List<SupplierOnboardingApplication> findByStatusAndDeletedFalse(SupplierOnboardingApplication.Status status);
}
