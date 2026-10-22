package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.InboxItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface InboxItemRepository extends JpaRepository<InboxItem, Long> {
    List<InboxItem> findByRecipientUsernameAndDeletedFalseOrderByCreatedAtDesc(String username);
    Optional<InboxItem> findByIdAndRecipientUsernameAndDeletedFalse(Long id, String username);
    boolean existsByRecipientUsernameAndBusinessTypeAndBusinessIdAndTitleAndDeletedFalse(
        String username, String businessType, String businessId, String title);
}
