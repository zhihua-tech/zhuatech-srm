/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.QualityIssue;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface QualityIssueRepository extends JpaRepository<QualityIssue, Long> {
    List<QualityIssue> findBySupplierIdAndDeletedFalse(Long supplierId);
    default List<QualityIssue> findBySupplierId(Long supplierId) { return findBySupplierIdAndDeletedFalse(supplierId); }
    Optional<QualityIssue> findByIdAndSupplierIdAndDeletedFalse(Long id, Long supplierId);
    List<QualityIssue> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    boolean existsByOrganizationCodeAndIssueNoAndDeletedFalse(String organizationCode, String issueNo);
}
