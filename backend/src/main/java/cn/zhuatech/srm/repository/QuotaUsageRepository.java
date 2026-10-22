package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.QuotaUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface QuotaUsageRepository extends JpaRepository<QuotaUsage, Long> {
    boolean existsByAllocationIdAndSourceTypeAndSourceIdAndDeletedFalse(Long allocationId, QuotaUsage.SourceType sourceType, String sourceId);
    List<QuotaUsage> findBySourceTypeAndSourceIdAndDeletedFalse(QuotaUsage.SourceType sourceType, String sourceId);
}
