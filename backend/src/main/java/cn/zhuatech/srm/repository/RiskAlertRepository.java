package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.RiskAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RiskAlertRepository extends JpaRepository<RiskAlert, Long> {
    List<RiskAlert> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    List<RiskAlert> findBySupplierIdAndDeletedFalse(Long supplierId);
    boolean existsByOrganizationCodeAndSupplierIdAndRuleCodeAndSourceIdAndStatusNotAndDeletedFalse(
        String organizationCode, Long supplierId, String ruleCode, String sourceId, RiskAlert.Status excludedStatus);
}
