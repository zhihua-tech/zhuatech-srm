package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierContact;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SupplierContactRepository extends JpaRepository<SupplierContact, Long> {
    List<SupplierContact> findBySupplierIdAndDeletedFalse(Long supplierId);
}
