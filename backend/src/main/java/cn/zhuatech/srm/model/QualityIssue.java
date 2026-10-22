/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** NCR/CAR 质量问题与 8D 整改闭环。 */
@Entity
@Table(name = "srm_quality_issue", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quality_org_no", columnNames = {"organization_code", "issue_no"})
}, indexes = {
    @Index(name = "idx_quality_org_supplier_status", columnList = "organization_code,supplier_id,status")
})
public class QualityIssue extends BaseEntity {
    public enum Type { NCR, CAR }
    public enum Status { OPEN, ANALYZING, ACTION_SUBMITTED, RETURNED, VERIFYING, CLOSED }
    @Column(nullable = false, length = 32) private String organizationCode = "DEFAULT";
    @Column(nullable = false, length = 32) private String issueNo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Type issueType = Type.NCR;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 150) private String title;
    @Column(nullable = false, length = 20) private String severity;
    @Column(nullable = false) private LocalDate dueDate;
    @Column(nullable = false, length = 40) private String ownerName;
    @Column(length = 2000) private String rootCause;
    @Column(length = 2000) private String interimAction;
    @Column(length = 2000) private String permanentAction;
    @Column(length = 2000) private String verificationResult;
    @Column(length = 500) private String returnReason;
    @Column(length = 64) private String evidenceFileKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    private LocalDateTime submittedAt;
    private LocalDateTime closedAt;

    protected QualityIssue() {}
    public QualityIssue(String organizationCode, String issueNo, Type issueType, Supplier supplier,
                        String title, String severity, LocalDate dueDate, String ownerName) {
        this.organizationCode = organizationCode; this.issueNo = issueNo; this.issueType = issueType;
        this.supplier = supplier; this.title = title; this.severity = severity;
        this.dueDate = dueDate; this.ownerName = ownerName; this.status = Status.OPEN;
    }
    /** 兼容旧版扩展代码。 */
    public QualityIssue(String no, Supplier supplier, String title, String severity, LocalDate dueDate,
                        String owner, Status status) {
        this("DEFAULT", no, Type.NCR, supplier, title, severity, dueDate, owner); this.status = status;
    }
    public void startAnalysis() { if (status != Status.OPEN && status != Status.RETURNED) throw new IllegalStateException("当前状态不可分析"); status = Status.ANALYZING; }
    public void submitAction(String rootCause, String interimAction, String permanentAction, String evidenceFileKey) {
        if (status != Status.ANALYZING && status != Status.RETURNED) throw new IllegalStateException("当前状态不可提交整改");
        this.rootCause = rootCause; this.interimAction = interimAction; this.permanentAction = permanentAction;
        this.evidenceFileKey = evidenceFileKey; this.status = Status.ACTION_SUBMITTED; this.submittedAt = LocalDateTime.now();
    }
    public void returnAction(String reason) { if (status != Status.ACTION_SUBMITTED && status != Status.VERIFYING) throw new IllegalStateException("整改未提交"); returnReason = reason; status = Status.RETURNED; }
    public void startVerification() { if (status != Status.ACTION_SUBMITTED) throw new IllegalStateException("整改未提交"); status = Status.VERIFYING; }
    public void close(String result) { if (status != Status.VERIFYING) throw new IllegalStateException("整改未进入验证"); verificationResult = result; status = Status.CLOSED; closedAt = LocalDateTime.now(); }
    public String getOrganizationCode() { return organizationCode; }
    public String getIssueNo() { return issueNo; }
    public Type getIssueType() { return issueType; }
    public Supplier getSupplier() { return supplier; }
    public String getTitle() { return title; }
    public String getSeverity() { return severity; }
    public LocalDate getDueDate() { return dueDate; }
    public String getOwnerName() { return ownerName; }
    public String getRootCause() { return rootCause; }
    public String getInterimAction() { return interimAction; }
    public String getPermanentAction() { return permanentAction; }
    public String getVerificationResult() { return verificationResult; }
    public String getReturnReason() { return returnReason; }
    public String getEvidenceFileKey() { return evidenceFileKey; }
    public Status getStatus() { return status; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
}
