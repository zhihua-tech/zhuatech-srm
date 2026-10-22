package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.PurchaseOrderLine;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PurchaseOrderLineRepository extends JpaRepository<PurchaseOrderLine, Long> {
    List<PurchaseOrderLine> findByOrderIdAndDeletedFalseOrderByLineNo(Long orderId);
}
