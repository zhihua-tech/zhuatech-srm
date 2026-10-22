package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierCertificate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
public interface SupplierCertificateRepository extends JpaRepository<SupplierCertificate, Long> {
    List<SupplierCertificate> findBySupplierIdAndDeletedFalse(Long supplierId);
    List<SupplierCertificate> findByExpiresOnBetweenAndDeletedFalse(LocalDate from, LocalDate to);
}
