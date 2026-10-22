package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.ContractRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface ContractRepository extends JpaRepository<ContractRecord, Long> {
    List<ContractRecord> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    List<ContractRecord> findBySupplierIdAndDeletedFalse(Long supplierId);
    List<ContractRecord> findByOrganizationCodeAndEffectiveToBetweenAndDeletedFalse(String organizationCode, LocalDate from, LocalDate to);
    long countByOrganizationCodeAndContractNoAndDeletedFalse(String organizationCode, String contractNo);
    List<ContractRecord> findByOrganizationCodeAndContractNoAndDeletedFalseOrderByContractVersionDesc(String organizationCode, String contractNo);
}
