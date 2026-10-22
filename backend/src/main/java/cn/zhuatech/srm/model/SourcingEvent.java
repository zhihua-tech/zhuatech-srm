/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** RFQ/RFP 询价项目头。 */
@Entity
@Table(name = "srm_sourcing_event", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sourcing_org_no", columnNames = {"organization_code", "event_no"})
}, indexes = {
    @Index(name = "idx_sourcing_org_status_deadline", columnList = "organization_code,status,deadline")
})
public class SourcingEvent extends BaseEntity {
    public enum Type { RFQ, RFP }
    public enum Status { DRAFT, PUBLISHED, OPEN, EVALUATING, PENDING_APPROVAL, AWARDED, CLOSED, CANCELLED }

    @Column(nullable = false, length = 32) private String organizationCode = "DEFAULT";
    @Column(nullable = false, length = 32) private String eventNo;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 8) private Type type = Type.RFQ;
    @Column(nullable = false, length = 120) private String title;
    @Column(nullable = false, length = 60) private String categoryName;
    @Column(nullable = false, length = 40) private String ownerName;
    @Column(length = 1000) private String requirementDescription;
    @Column(nullable = false) private LocalDateTime deadline;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal budget;
    @Column(nullable = false) private int invitedCount;
    @Column(nullable = false) private int respondedCount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    private LocalDateTime publishedAt;
    @Column(name = "proposed_quote_id") private Long proposedQuoteId;
    @Column(length = 500) private String proposedAwardReason;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "awarded_supplier_id") private Supplier awardedSupplier;
    @Column(length = 500) private String awardReason;

    protected SourcingEvent() {}

    public SourcingEvent(String organizationCode, String eventNo, Type type, String title, String categoryName,
                         String ownerName, String requirementDescription, LocalDateTime deadline, BigDecimal budget) {
        this.organizationCode = organizationCode; this.eventNo = eventNo; this.type = type; this.title = title;
        this.categoryName = categoryName; this.ownerName = ownerName; this.requirementDescription = requirementDescription;
        this.deadline = deadline; this.budget = budget; this.status = Status.DRAFT;
    }

    /** 兼容旧版扩展代码。 */
    public SourcingEvent(String no, String title, String category, String owner, LocalDateTime deadline,
                         BigDecimal budget, int invited, int responded, Status status) {
        this("DEFAULT", no, Type.RFQ, title, category, owner, null, deadline, budget);
        this.invitedCount = invited; this.respondedCount = responded; this.status = status;
    }

    public void publish(int invitationCount) {
        if (status != Status.DRAFT) throw new IllegalStateException("仅草稿询价可以发布");
        if (invitationCount < 1) throw new IllegalStateException("至少邀请一家供应商");
        invitedCount = invitationCount; status = Status.OPEN; publishedAt = LocalDateTime.now();
    }
    public void markResponded() { respondedCount = Math.min(invitedCount, respondedCount + 1); }
    public void startEvaluation() { if (status != Status.OPEN) throw new IllegalStateException("询价未开放"); status = Status.EVALUATING; }
    public void requestAwardApproval(Long quoteId, String reason) { if (status != Status.EVALUATING) throw new IllegalStateException("尚未进入评审"); proposedQuoteId = quoteId; proposedAwardReason = reason; status = Status.PENDING_APPROVAL; }
    public void rejectAwardApproval() { if (status != Status.PENDING_APPROVAL) throw new IllegalStateException("当前没有待审批定标申请"); proposedQuoteId = null; proposedAwardReason = null; status = Status.EVALUATING; }
    public void award(Supplier supplier) { if (status != Status.PENDING_APPROVAL) throw new IllegalStateException("当前状态不可定标"); awardedSupplier = supplier; awardReason = proposedAwardReason; status = Status.AWARDED; }

    public String getOrganizationCode() { return organizationCode; }
    public String getEventNo() { return eventNo; }
    public Type getType() { return type; }
    public String getTitle() { return title; }
    public String getCategoryName() { return categoryName; }
    public String getOwnerName() { return ownerName; }
    public String getRequirementDescription() { return requirementDescription; }
    public LocalDateTime getDeadline() { return deadline; }
    public BigDecimal getBudget() { return budget; }
    public int getInvitedCount() { return invitedCount; }
    public int getRespondedCount() { return respondedCount; }
    public Status getStatus() { return status; }
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public Long getProposedQuoteId() { return proposedQuoteId; }
    public String getProposedAwardReason() { return proposedAwardReason; }
    public Supplier getAwardedSupplier() { return awardedSupplier; }
    public String getAwardReason() { return awardReason; }
}
