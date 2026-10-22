/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 规则型供应商风险预警和整改跟踪。 */
@Entity @Table(name = "srm_risk_alert")
public class RiskAlert extends BaseEntity {
    public enum Status { OPEN, ACTION_ASSIGNED, VERIFYING, CLOSED }
    @Column(nullable = false, length = 32) private String organizationCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 40) private String ruleCode;
    @Column(length = 40) private String sourceType;
    @Column(length = 80) private String sourceId;
    @Column(nullable = false, length = 16) private String riskLevel;
    @Column(nullable = false, length = 500) private String description;
    @Column(length = 64) private String ownerUsername;
    @Column(length = 1000) private String correctiveAction;
    private LocalDateTime dueAt;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    protected RiskAlert() {}
    public RiskAlert(String organizationCode, Supplier supplier, String ruleCode, String riskLevel,
                     String description, String ownerUsername, LocalDateTime dueAt) {
        this(organizationCode, supplier, ruleCode, null, null, riskLevel, description, ownerUsername, dueAt);
    }
    public RiskAlert(String organizationCode, Supplier supplier, String ruleCode, String sourceType, String sourceId,
                     String riskLevel, String description, String ownerUsername, LocalDateTime dueAt) {
        this.organizationCode = organizationCode; this.supplier = supplier; this.ruleCode = ruleCode;
        this.sourceType = sourceType; this.sourceId = sourceId;
        this.riskLevel = riskLevel; this.description = description; this.ownerUsername = ownerUsername;
        this.dueAt = dueAt; this.status = Status.OPEN;
    }
    public void assignAction(String action, String owner, LocalDateTime dueAt) { correctiveAction = action; ownerUsername = owner; this.dueAt = dueAt; status = Status.ACTION_ASSIGNED; }
    public void verify() { if (status != Status.ACTION_ASSIGNED) throw new IllegalStateException("整改尚未提交"); status = Status.VERIFYING; }
    public void close() { if (status != Status.VERIFYING) throw new IllegalStateException("风险尚未验证"); status = Status.CLOSED; }
    public String getOrganizationCode() { return organizationCode; }
    public Supplier getSupplier() { return supplier; }
    public String getRuleCode() { return ruleCode; }
    public String getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public String getRiskLevel() { return riskLevel; }
    public String getDescription() { return description; }
    public String getOwnerUsername() { return ownerUsername; }
    public String getCorrectiveAction() { return correctiveAction; }
    public LocalDateTime getDueAt() { return dueAt; }
    public Status getStatus() { return status; }
}
