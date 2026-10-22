/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 有时效且只能使用一次的供应商注册邀请。 */
@Entity
@Table(name = "srm_supplier_invitation")
public class SupplierInvitation extends BaseEntity {
    public enum Status { CREATED, SENT, ACCEPTED, EXPIRED, REVOKED }
    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, unique = true, length = 64) private String token;
    @Column(nullable = false, length = 120) private String supplierName;
    @Column(nullable = false, length = 120) private String email;
    @Column(length = 40) private String contactName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
    @Column(nullable = false) private LocalDateTime expiresAt;
    private LocalDateTime acceptedAt;

    protected SupplierInvitation() {}
    public SupplierInvitation(String organizationCode, String token, String supplierName, String email,
                              String contactName, LocalDateTime expiresAt) {
        this.organizationCode = organizationCode; this.token = token; this.supplierName = supplierName;
        this.email = email; this.contactName = contactName; this.expiresAt = expiresAt; this.status = Status.CREATED;
    }
    public void markSent() { status = Status.SENT; }
    public void accept() {
        if (expiresAt.isBefore(LocalDateTime.now())) { status = Status.EXPIRED; throw new IllegalStateException("邀请已过期"); }
        if (status == Status.ACCEPTED || status == Status.REVOKED) throw new IllegalStateException("邀请不可用");
        status = Status.ACCEPTED; acceptedAt = LocalDateTime.now();
    }
    public String getOrganizationCode() { return organizationCode; }
    public String getToken() { return token; }
    public String getSupplierName() { return supplierName; }
    public String getEmail() { return email; }
    public String getContactName() { return contactName; }
    public Status getStatus() { return status; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getAcceptedAt() { return acceptedAt; }
}
