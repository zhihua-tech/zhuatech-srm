/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 合同台账及版本。 */
@Entity
@Table(name = "srm_contract", uniqueConstraints = {
    @UniqueConstraint(name = "uk_contract_org_no_version", columnNames = {"organization_code", "contract_no", "contract_version"})
})
public class ContractRecord extends BaseEntity {
    public enum Status { DRAFT, PENDING_APPROVAL, APPROVED, ACTIVE, SUPERSEDED, EXPIRED, TERMINATED }
    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, length = 40) private String contractNo;
    @Column(name = "contract_version", nullable = false) private int contractVersion;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 160) private String title;
    @Column(nullable = false, precision = 16, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 8) private String currency;
    @Column(nullable = false) private LocalDate effectiveFrom;
    @Column(nullable = false) private LocalDate effectiveTo;
    @Column(length = 64) private String attachmentKey;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    @Column(length = 500) private String changeReason;
    protected ContractRecord() {}
    public ContractRecord(String organizationCode, String contractNo, int contractVersion, Supplier supplier,
                          String title, BigDecimal amount, String currency, LocalDate effectiveFrom,
                          LocalDate effectiveTo, String attachmentKey, String changeReason) {
        this.organizationCode = organizationCode; this.contractNo = contractNo; this.contractVersion = contractVersion;
        this.supplier = supplier; this.title = title; this.amount = amount; this.currency = currency;
        this.effectiveFrom = effectiveFrom; this.effectiveTo = effectiveTo; this.attachmentKey = attachmentKey;
        this.changeReason = changeReason; this.status = Status.DRAFT;
    }
    public void submit() { if (status != Status.DRAFT) throw new IllegalStateException("仅草稿可提交"); status = Status.PENDING_APPROVAL; }
    public void approve() { if (status != Status.PENDING_APPROVAL) throw new IllegalStateException("合同未提交审批"); status = Status.APPROVED; }
    public void activate() { if (status != Status.APPROVED) throw new IllegalStateException("合同未批准"); status = Status.ACTIVE; }
    public void supersede() { if (status != Status.ACTIVE) throw new IllegalStateException("仅生效合同可被新版本替代"); status = Status.SUPERSEDED; }
    public void expire() { if (status != Status.ACTIVE) throw new IllegalStateException("仅生效合同可失效"); status = Status.EXPIRED; }
    public void terminate() { if (status == Status.TERMINATED || status == Status.SUPERSEDED) throw new IllegalStateException("合同已终止或被替代"); status = Status.TERMINATED; }
    public String getOrganizationCode() { return organizationCode; }
    public String getContractNo() { return contractNo; }
    public int getContractVersion() { return contractVersion; }
    public Supplier getSupplier() { return supplier; }
    public String getTitle() { return title; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public LocalDate getEffectiveTo() { return effectiveTo; }
    public String getAttachmentKey() { return attachmentKey; }
    public Status getStatus() { return status; }
    public String getChangeReason() { return changeReason; }
}
