package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SourcingItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SourcingItemRepository extends JpaRepository<SourcingItem, Long> {
    List<SourcingItem> findByEventIdAndDeletedFalseOrderByLineNo(Long eventId);
}
