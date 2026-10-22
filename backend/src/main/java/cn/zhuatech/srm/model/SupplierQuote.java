/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 供应商报价版本。相同询价和供应商可保留多个不可覆盖的版本。 */
@Entity
@Table(name = "srm_quote", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quote_event_supplier_version", columnNames = {"event_id", "supplier_id", "quote_version"})
})
public class SupplierQuote extends BaseEntity {
    public enum Status { DRAFT, SUBMITTED, WITHDRAWN, SHORTLISTED, REJECTED, AWARDED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private SourcingEvent event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(name = "quote_version", nullable = false) private int quoteVersion;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal taxRate;
    @Column(nullable = false) private int leadDays;
    @Column(nullable = false) private LocalDate validUntil;
    @Column(length = 500) private String remark;
    @Column(length = 64) private String attachmentKey;
    @Column(precision = 6, scale = 2) private BigDecimal evaluationScore;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;

    protected SupplierQuote() {}
    public SupplierQuote(SourcingEvent event, Supplier supplier, int quoteVersion, BigDecimal amount,
                         BigDecimal taxRate, int leadDays, LocalDate validUntil, String remark, String attachmentKey) {
        this.event = event; this.supplier = supplier; this.quoteVersion = quoteVersion; this.amount = amount;
        this.taxRate = taxRate; this.leadDays = leadDays; this.validUntil = validUntil;
        this.remark = remark; this.attachmentKey = attachmentKey; this.status = Status.SUBMITTED;
    }
    /** 兼容旧版扩展代码。 */
    public SupplierQuote(SourcingEvent event, Supplier supplier, BigDecimal amount, int leadDays,
                         LocalDate validUntil, String remark) {
        this(event, supplier, 1, amount, new BigDecimal("13.00"), leadDays, validUntil, remark, null);
    }
    public void score(BigDecimal score) { if (status != Status.SUBMITTED && status != Status.SHORTLISTED) throw new IllegalStateException("报价不可评审"); evaluationScore = score; status = Status.SHORTLISTED; }
    public void markAwarded() { status = Status.AWARDED; }
    public SourcingEvent getEvent() { return event; }
    public Supplier getSupplier() { return supplier; }
    public int getQuoteVersion() { return quoteVersion; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getTaxRate() { return taxRate; }
    public int getLeadDays() { return leadDays; }
    public LocalDate getValidUntil() { return validUntil; }
    public String getRemark() { return remark; }
    public String getAttachmentKey() { return attachmentKey; }
    public BigDecimal getEvaluationScore() { return evaluationScore; }
    public Status getStatus() { return status; }
}
