package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.QuotaPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
public interface QuotaPlanRepository extends JpaRepository<QuotaPlan, Long> {
    Page<QuotaPlan> findByOrganizationCodeAndDeletedFalse(String organizationCode, Pageable pageable);
    Optional<QuotaPlan> findByIdAndOrganizationCodeAndDeletedFalse(Long id, String organizationCode);
    List<QuotaPlan> findByOrganizationCodeAndProcurementOrganizationAndFactoryCodeAndMaterialCodeAndStatusAndDeletedFalse(
        String organizationCode, String procurementOrganization, String factoryCode, String materialCode, QuotaPlan.Status status);
    List<QuotaPlan> findByOrganizationCodeAndMaterialCodeAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqualAndDeletedFalse(
        String organizationCode, String materialCode, QuotaPlan.Status status, LocalDate validFrom, LocalDate validTo);
    List<QuotaPlan> findByOrganizationCodeAndProcurementOrganizationAndFactoryCodeAndMaterialCodeAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqualAndDeletedFalse(
        String organizationCode, String procurementOrganization, String factoryCode, String materialCode,
        QuotaPlan.Status status, LocalDate date1, LocalDate date2);
}
