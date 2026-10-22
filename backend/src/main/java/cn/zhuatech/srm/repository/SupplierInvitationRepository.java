package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.SupplierInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SupplierInvitationRepository extends JpaRepository<SupplierInvitation, Long> {
    Optional<SupplierInvitation> findByTokenAndDeletedFalse(String token);
}
