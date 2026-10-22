/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SupplierRepository extends JpaRepository<Supplier, Long> {
    Page<Supplier> findByOrganizationCodeAndDeletedFalse(String organizationCode, Pageable pageable);
    List<Supplier> findByOrganizationCodeAndDeletedFalse(String organizationCode);
    Optional<Supplier> findByIdAndOrganizationCodeAndDeletedFalse(Long id, String organizationCode);
    Optional<Supplier> findBySupplierCodeAndOrganizationCodeAndDeletedFalse(String supplierCode, String organizationCode);
    boolean existsByOrganizationCodeAndRegistrationNoAndDeletedFalse(String organizationCode, String registrationNo);
    boolean existsByOrganizationCodeAndSupplierNameIgnoreCaseAndDeletedFalse(String organizationCode, String supplierName);
}
