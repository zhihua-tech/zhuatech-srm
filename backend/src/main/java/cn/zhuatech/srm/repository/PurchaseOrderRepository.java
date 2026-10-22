/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.PurchaseOrder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    List<PurchaseOrder> findBySupplierIdAndDeletedFalseOrderByDeliveryDate(Long supplierId);
    default List<PurchaseOrder> findBySupplierIdOrderByDeliveryDate(Long supplierId) { return findBySupplierIdAndDeletedFalseOrderByDeliveryDate(supplierId); }
    Optional<PurchaseOrder> findByIdAndSupplierIdAndDeletedFalse(Long id, Long supplierId);
    Optional<PurchaseOrder> findByOrderNoAndOrganizationCodeAndDeletedFalse(String orderNo, String organizationCode);
    Page<PurchaseOrder> findByOrganizationCodeAndDeletedFalse(String organizationCode, Pageable pageable);
    List<PurchaseOrder> findBySupplierIdAndDeliveryDateBetweenAndDeletedFalse(Long supplierId, LocalDate from, LocalDate to);
    List<PurchaseOrder> findByDeletedFalse();
}
