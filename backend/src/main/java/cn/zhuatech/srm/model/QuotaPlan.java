/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 采购配额方案及版本。 */
@Entity
@Table(name = "srm_quota_plan", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quota_scope_version", columnNames = {
        "organization_code", "procurement_organization", "factory_code", "material_code",
        "valid_from", "valid_to", "plan_version"
    })
}, indexes = {@Index(name = "idx_quota_scope_status", columnList = "organization_code,procurement_organization,factory_code,material_code,status")})
public class QuotaPlan extends BaseEntity {
    public enum Type { RATIO, QUANTITY }
    public enum Status { DRAFT, ACTIVE, EXPIRED, CANCELLED }
    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, length = 32) private String procurementOrganization;
    @Column(nullable = false, length = 32) private String factoryCode;
    @Column(nullable = false, length = 64) private String materialCode;
    @Column(nullable = false, length = 160) private String materialName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Type quotaType;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal totalQuantity;
    @Column(nullable = false) private LocalDate validFrom;
    @Column(nullable = false) private LocalDate validTo;
    @Column(name = "plan_version", nullable = false) private int planVersion;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
    @Column(length = 500) private String changeReason;
    @Column(length = 64) private String activatedBy;
    private LocalDateTime activatedAt;
    protected QuotaPlan() {}
    public QuotaPlan(String organizationCode, String procurementOrganization, String factoryCode,
                     String materialCode, String materialName, Type quotaType, BigDecimal totalQuantity,
                     LocalDate validFrom, LocalDate validTo, int planVersion, String changeReason) {
        this.organizationCode = organizationCode; this.procurementOrganization = procurementOrganization;
        this.factoryCode = factoryCode; this.materialCode = materialCode; this.materialName = materialName;
        this.quotaType = quotaType; this.totalQuantity = totalQuantity; this.validFrom = validFrom;
        this.validTo = validTo; this.planVersion = planVersion; this.changeReason = changeReason;
        this.status = Status.DRAFT;
    }
    public void activate(String actor) { if (status != Status.DRAFT) throw new IllegalStateException("仅草稿配额可以生效"); status = Status.ACTIVE; activatedBy = actor; activatedAt = LocalDateTime.now(); }
    public void expire() { if (status == Status.ACTIVE) status = Status.EXPIRED; }
    public String getOrganizationCode() { return organizationCode; }
    public String getProcurementOrganization() { return procurementOrganization; }
    public String getFactoryCode() { return factoryCode; }
    public String getMaterialCode() { return materialCode; }
    public String getMaterialName() { return materialName; }
    public Type getQuotaType() { return quotaType; }
    public BigDecimal getTotalQuantity() { return totalQuantity; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getValidTo() { return validTo; }
    public int getPlanVersion() { return planVersion; }
    public Status getStatus() { return status; }
    public String getChangeReason() { return changeReason; }
    public String getActivatedBy() { return activatedBy; }
    public LocalDateTime getActivatedAt() { return activatedAt; }
}
