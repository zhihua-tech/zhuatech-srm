/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * SRM 用户账号。角色决定菜单和操作权限，organizationCode 与 supplier 共同决定数据范围。
 * 商业授权或定制开发请微信添加微信号 zhuatech 或 zhuatech2 进行咨询。
 */
@Entity
@Table(name = "srm_user")
public class UserAccount extends BaseEntity {
    public enum Role { ADMIN, BUYER, QUALITY, FINANCE, AUDITOR, SUPPLIER }

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(nullable = false, length = 50)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false, length = 32)
    private String organizationCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false)
    private int failedLoginAttempts;

    private LocalDateTime lockedUntil;
    private LocalDateTime passwordChangedAt;
    private LocalDateTime lastLoginAt;

    protected UserAccount() {}

    public UserAccount(String username, String password, String fullName, Role role,
                       String organizationCode, Supplier supplier) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.organizationCode = organizationCode;
        this.supplier = supplier;
        this.passwordChangedAt = LocalDateTime.now();
    }

    /** 兼容既有扩展代码，默认采购组织为 DEFAULT。 */
    public UserAccount(String username, String password, String fullName, Role role, Supplier supplier) {
        this(username, password, fullName, role, "DEFAULT", supplier);
    }

    public boolean isLocked() {
        return lockedUntil != null && lockedUntil.isAfter(LocalDateTime.now());
    }

    public void recordLoginFailure(int maxAttempts, int lockMinutes) {
        failedLoginAttempts++;
        if (failedLoginAttempts >= maxAttempts) {
            lockedUntil = LocalDateTime.now().plusMinutes(lockMinutes);
            failedLoginAttempts = 0;
        }
    }

    public void recordLoginSuccess() {
        failedLoginAttempts = 0;
        lockedUntil = null;
        lastLoginAt = LocalDateTime.now();
    }

    public void changePassword(String encodedPassword) {
        password = encodedPassword;
        passwordChangedAt = LocalDateTime.now();
    }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getFullName() { return fullName; }
    public Role getRole() { return role; }
    public String getOrganizationCode() { return organizationCode; }
    public Supplier getSupplier() { return supplier; }
    public boolean isEnabled() { return enabled; }
    public int getFailedLoginAttempts() { return failedLoginAttempts; }
    public LocalDateTime getLockedUntil() { return lockedUntil; }
    public LocalDateTime getPasswordChangedAt() { return passwordChangedAt; }
    public LocalDateTime getLastLoginAt() { return lastLoginAt; }
}
