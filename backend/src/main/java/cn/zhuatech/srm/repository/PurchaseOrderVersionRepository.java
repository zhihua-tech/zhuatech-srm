package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.PurchaseOrderVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface PurchaseOrderVersionRepository extends JpaRepository<PurchaseOrderVersion, Long> {
    List<PurchaseOrderVersion> findByOrderIdAndDeletedFalseOrderByOrderVersionDesc(Long orderId);
}
