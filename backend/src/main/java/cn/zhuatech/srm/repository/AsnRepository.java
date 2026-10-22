package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.AdvanceShippingNotice;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface AsnRepository extends JpaRepository<AdvanceShippingNotice, Long> {
    List<AdvanceShippingNotice> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    List<AdvanceShippingNotice> findBySupplierIdAndDeletedFalse(Long supplierId);
    Optional<AdvanceShippingNotice> findByIdAndSupplierIdAndDeletedFalse(Long id, Long supplierId);
}
