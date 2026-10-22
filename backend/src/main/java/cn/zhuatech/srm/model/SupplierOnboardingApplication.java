/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 标准准入问卷和固定两级审批单。 */
@Entity
@Table(name = "srm_supplier_onboarding")
public class SupplierOnboardingApplication extends BaseEntity {
    public enum Status { DRAFT, SUBMITTED, FIRST_APPROVED, APPROVED, REJECTED, RETURNED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, columnDefinition = "TEXT") private String questionnaireJson;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(length = 64) private String firstApprover;
    @Column(length = 64) private String finalApprover;
    @Column(length = 500) private String approvalComment;
    private LocalDateTime submittedAt;
    private LocalDateTime approvedAt;
    protected SupplierOnboardingApplication() {}
    public SupplierOnboardingApplication(Supplier supplier, String questionnaireJson) {
        this.supplier = supplier; this.questionnaireJson = questionnaireJson; this.status = Status.DRAFT;
    }
    public void updateQuestionnaire(String value) {
        if (status != Status.DRAFT && status != Status.RETURNED) throw new IllegalStateException("当前状态不可修改问卷");
        questionnaireJson = value;
    }
    public void submit() { if (status != Status.DRAFT && status != Status.RETURNED) throw new IllegalStateException("当前状态不可提交"); status = Status.SUBMITTED; submittedAt = LocalDateTime.now(); }
    public void firstApprove(String actor, String comment) { if (status != Status.SUBMITTED) throw new IllegalStateException("未进入初审"); firstApprover = actor; approvalComment = comment; status = Status.FIRST_APPROVED; }
    public void finalApprove(String actor, String comment) { if (status != Status.FIRST_APPROVED) throw new IllegalStateException("未通过初审"); finalApprover = actor; approvalComment = comment; status = Status.APPROVED; approvedAt = LocalDateTime.now(); }
    public void returnForRevision(String actor, String comment) { finalApprover = actor; approvalComment = comment; status = Status.RETURNED; }
    public Supplier getSupplier() { return supplier; }
    public String getQuestionnaireJson() { return questionnaireJson; }
    public Status getStatus() { return status; }
    public String getFirstApprover() { return firstApprover; }
    public String getFinalApprover() { return finalApprover; }
    public String getApprovalComment() { return approvalComment; }
    public LocalDateTime getSubmittedAt() { return submittedAt; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
}
