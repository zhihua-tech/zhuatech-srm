package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.ReconciliationStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface ReconciliationRepository extends JpaRepository<ReconciliationStatement, Long> {
    List<ReconciliationStatement> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    List<ReconciliationStatement> findBySupplierIdAndDeletedFalse(Long supplierId);
    Optional<ReconciliationStatement> findByIdAndSupplierIdAndDeletedFalse(Long id, Long supplierId);
    boolean existsByOrganizationCodeAndStatementNoAndDeletedFalse(String organizationCode, String statementNo);
}
