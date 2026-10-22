package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SourcingInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface SourcingInvitationRepository extends JpaRepository<SourcingInvitation, Long> {
    boolean existsByEventIdAndSupplierIdAndDeletedFalse(Long eventId, Long supplierId);
    List<SourcingInvitation> findBySupplierIdAndDeletedFalse(Long supplierId);
    List<SourcingInvitation> findByEventIdAndDeletedFalse(Long eventId);
}
