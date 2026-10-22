package cn.zhuatech.srm.repository;
import cn.zhuatech.srm.model.RolePermission;
import cn.zhuatech.srm.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
    List<RolePermission> findByOrganizationCodeAndRoleAndDeletedFalse(String organizationCode, UserAccount.Role role);
}
