/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;

/** 组织级角色权限覆盖；未配置时使用系统安全基线。 */
@Entity
@Table(name = "srm_role_permission", indexes = {
    @Index(name = "idx_role_permission_scope", columnList = "organization_code,role,enabled")
})
public class RolePermission extends BaseEntity {
    @Column(nullable = false, length = 32) private String organizationCode;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private UserAccount.Role role;
    @Column(nullable = false, length = 64) private String permissionCode;
    @Column(nullable = false) private boolean enabled = true;
    protected RolePermission() {}
    public RolePermission(String organizationCode, UserAccount.Role role, String permissionCode) {
        this.organizationCode = organizationCode; this.role = role; this.permissionCode = permissionCode;
    }
    public void setEnabled(boolean value) { enabled = value; }
    public String getOrganizationCode() { return organizationCode; }
    public UserAccount.Role getRole() { return role; }
    public String getPermissionCode() { return permissionCode; }
    public boolean isEnabled() { return enabled; }
}
