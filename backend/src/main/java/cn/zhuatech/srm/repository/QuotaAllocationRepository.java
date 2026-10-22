package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.QuotaAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface QuotaAllocationRepository extends JpaRepository<QuotaAllocation, Long> {
    List<QuotaAllocation> findByPlanIdAndDeletedFalse(Long planId);
    Optional<QuotaAllocation> findByPlanIdAndSupplierIdAndDeletedFalse(Long planId, Long supplierId);
}
