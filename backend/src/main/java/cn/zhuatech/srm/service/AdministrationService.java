/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** 组织、账号、角色权限和账号启停管理。 */
@Service
@Transactional(readOnly = true)
public class AdministrationService {
    private final OrganizationRepository organizations;
    private final UserRepository users;
    private final SupplierRepository suppliers;
    private final RolePermissionRepository rolePermissions;
    private final PasswordEncoder encoder;
    private final CurrentUserService current;
    private final PermissionService permissions;
    private final AuditService audit;

    public AdministrationService(OrganizationRepository organizations, UserRepository users,
        SupplierRepository suppliers, RolePermissionRepository rolePermissions, PasswordEncoder encoder,
        CurrentUserService current, PermissionService permissions, AuditService audit) {
        this.organizations = organizations; this.users = users; this.suppliers = suppliers;
        this.rolePermissions = rolePermissions; this.encoder = encoder; this.current = current;
        this.permissions = permissions; this.audit = audit;
    }

    public List<OrganizationView> organizations() {
        return organizations.findByDeletedFalseOrderByOrganizationCode().stream().map(OrganizationView::from).toList();
    }

    @Transactional
    public OrganizationView createOrganization(OrganizationRequest request) {
        if (organizations.findByOrganizationCodeAndDeletedFalse(request.code()).isPresent()) throw new BusinessException("组织编码已存在");
        Organization value = organizations.save(new Organization(request.code(), request.name()));
        audit.record(current.organizationCode(), current.get().getUsername(), "ADMIN", "ORGANIZATION_CREATE",
            "ORGANIZATION", value.getId(), null, request);
        return OrganizationView.from(value);
    }

    public List<UserView> users() {
        return users.findByOrganizationCodeAndDeletedFalse(current.organizationCode()).stream().map(UserView::from).toList();
    }

    @Transactional
    public UserView createUser(UserRequest request) {
        if (users.findByUsername(request.username()).isPresent()) throw new BusinessException("用户名已存在");
        Supplier supplier = null;
        if (request.role() == UserAccount.Role.SUPPLIER) {
            if (request.supplierId() == null) throw new BusinessException("供应商账号必须关联供应商");
            supplier = suppliers.findByIdAndOrganizationCodeAndDeletedFalse(request.supplierId(), current.organizationCode())
                .orElseThrow(() -> new BusinessException("供应商不存在"));
        }
        UserAccount user = users.save(new UserAccount(request.username(), encoder.encode(request.password()),
            request.fullName(), request.role(), current.organizationCode(), supplier));
        audit.record(current.organizationCode(), current.get().getUsername(), "ADMIN", "USER_CREATE", "USER", user.getId(),
            null, Map.of("username", user.getUsername(), "role", user.getRole()));
        return UserView.from(user);
    }

    @Transactional
    public UserView setEnabled(Long id, boolean enabled) {
        UserAccount user = users.findById(id).filter(value -> value.getOrganizationCode().equals(current.organizationCode()))
            .orElseThrow(() -> new BusinessException("用户不存在"));
        if (user.getId().equals(current.get().getId()) && !enabled) throw new BusinessException("不能停用当前账号");
        user.setEnabled(enabled);
        audit.record(current.organizationCode(), current.get().getUsername(), "ADMIN", enabled ? "USER_ENABLE" : "USER_DISABLE",
            "USER", id, null, enabled);
        return UserView.from(user);
    }

    public RolePermissionView rolePermissions(UserAccount.Role role) {
        List<RolePermission> configured = rolePermissions.findByOrganizationCodeAndRoleAndDeletedFalse(current.organizationCode(), role);
        Set<String> active = configured.isEmpty() ? permissions.defaultPermissions(role)
            : configured.stream().filter(RolePermission::isEnabled).map(RolePermission::getPermissionCode)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        return new RolePermissionView(role, configured.isEmpty(), active);
    }

    @Transactional
    public RolePermissionView replaceRolePermissions(UserAccount.Role role, RolePermissionRequest request) {
        if (role == UserAccount.Role.ADMIN && !request.permissions().contains("*")) {
            throw new BusinessException("管理员角色必须保留全部权限");
        }
        Set<String> before = new TreeSet<>(rolePermissions(role).permissions());
        List<RolePermission> configured = rolePermissions.findByOrganizationCodeAndRoleAndDeletedFalse(current.organizationCode(), role);
        configured.forEach(value -> value.setEnabled(request.permissions().contains(value.getPermissionCode())));
        Set<String> existing = configured.stream().map(RolePermission::getPermissionCode).collect(java.util.stream.Collectors.toSet());
        for (String code : request.permissions()) {
            if (code.isBlank() || code.length() > 64) throw new BusinessException("权限编码不合法");
            if (!existing.contains(code)) rolePermissions.save(new RolePermission(current.organizationCode(), role, code));
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "ADMIN", "ROLE_PERMISSION_REPLACE",
            "ROLE", role, before, request.permissions());
        return rolePermissions(role);
    }

    public record OrganizationRequest(@Pattern(regexp = "^[A-Z0-9_-]{2,32}$") String code, @NotBlank String name) {}
    public record UserRequest(@NotBlank String username,
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{12,72}$") String password,
        @NotBlank String fullName, @NotNull UserAccount.Role role, Long supplierId) {}
    public record RolePermissionRequest(@NotNull Set<@NotBlank String> permissions) {}
    public record OrganizationView(Long id, String code, String name, boolean enabled) {
        static OrganizationView from(Organization value) { return new OrganizationView(value.getId(), value.getOrganizationCode(), value.getOrganizationName(), value.isEnabled()); }
    }
    public record UserView(Long id, String username, String fullName, String role, boolean enabled, Long supplierId) {
        static UserView from(UserAccount value) { return new UserView(value.getId(), value.getUsername(), value.getFullName(), value.getRole().name(), value.isEnabled(), value.getSupplier() == null ? null : value.getSupplier().getId()); }
    }
    public record RolePermissionView(UserAccount.Role role, boolean usingDefaults, Set<String> permissions) {}
}
