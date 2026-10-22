/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierQuote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface SupplierQuoteRepository extends JpaRepository<SupplierQuote, Long> {
    long countBySupplierId(Long supplierId);
    long countByEventIdAndSupplierId(Long eventId, Long supplierId);
    List<SupplierQuote> findByEventIdAndDeletedFalseOrderByAmountAsc(Long eventId);
    List<SupplierQuote> findByEventIdAndSupplierIdAndDeletedFalseOrderByQuoteVersionDesc(Long eventId, Long supplierId);
    Optional<SupplierQuote> findFirstByEventIdAndSupplierIdAndDeletedFalseOrderByQuoteVersionDesc(Long eventId, Long supplierId);
}
