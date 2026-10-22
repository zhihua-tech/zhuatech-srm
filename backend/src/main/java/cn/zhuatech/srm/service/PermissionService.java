/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.model.UserAccount;
import cn.zhuatech.srm.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;
import java.util.EnumMap;
import java.util.Set;

/** 服务端权限矩阵。前端菜单仅用于呈现，所有接口仍需调用本权限矩阵。 */
@Service("permissionService")
public class PermissionService {
    private static final Set<String> ALL = Set.of("*");
    private static final EnumMap<UserAccount.Role, Set<String>> MATRIX = new EnumMap<>(UserAccount.Role.class);
    static {
        MATRIX.put(UserAccount.Role.ADMIN, ALL);
        MATRIX.put(UserAccount.Role.BUYER, Set.of("DASHBOARD", "SUPPLIER_READ", "SUPPLIER_MANAGE", "SOURCING_MANAGE",
            "CONTRACT_MANAGE", "ORDER_MANAGE", "QUOTA_MANAGE", "DELIVERY_READ", "PERFORMANCE_MANAGE",
            "IMPORT", "EXPORT", "FILE", "MESSAGE"));
        MATRIX.put(UserAccount.Role.QUALITY, Set.of("DASHBOARD", "SUPPLIER_READ", "QUALITY_MANAGE",
            "PERFORMANCE_MANAGE", "EXPORT", "FILE", "MESSAGE"));
        MATRIX.put(UserAccount.Role.FINANCE, Set.of("DASHBOARD", "SUPPLIER_READ", "CONTRACT_READ",
            "RECONCILIATION_MANAGE", "INVOICE_MANAGE", "EXPORT", "FILE", "MESSAGE"));
        MATRIX.put(UserAccount.Role.AUDITOR, Set.of("DASHBOARD", "SUPPLIER_READ", "SOURCING_READ", "CONTRACT_READ",
            "ORDER_READ", "QUOTA_READ", "DELIVERY_READ", "QUALITY_READ", "RECONCILIATION_READ",
            "PERFORMANCE_READ", "AUDIT_READ", "EXPORT", "FILE", "MESSAGE"));
        MATRIX.put(UserAccount.Role.SUPPLIER, Set.of("PORTAL", "SUPPLIER_SELF_MANAGE", "QUOTE_SUBMIT",
            "ORDER_CONFIRM", "ASN_MANAGE", "QUALITY_ACTION", "RECONCILIATION_CONFIRM", "FILE", "MESSAGE"));
    }

    private final CurrentUserService current;
    private final RolePermissionRepository overrides;
    public PermissionService(CurrentUserService current, RolePermissionRepository overrides) { this.current = current; this.overrides = overrides; }
    public boolean has(String permission) {
        Set<String> permissions = currentPermissions();
        return permissions.contains("*") || permissions.contains(permission);
    }
    public Set<String> currentPermissions() {
        UserAccount user = current.get();
        var configured = overrides.findByOrganizationCodeAndRoleAndDeletedFalse(user.getOrganizationCode(), user.getRole());
        Set<String> custom = configured.stream()
            .filter(cn.zhuatech.srm.model.RolePermission::isEnabled)
            .map(cn.zhuatech.srm.model.RolePermission::getPermissionCode)
            .collect(java.util.stream.Collectors.toUnmodifiableSet());
        Set<String> values = configured.isEmpty() ? MATRIX.getOrDefault(user.getRole(), Set.of()) : custom;
        return values.contains("*") ? Set.of("*") : values;
    }
    public Set<String> defaultPermissions(UserAccount.Role role) { return MATRIX.getOrDefault(role, Set.of()); }
}
