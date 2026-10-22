/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 发票登记，不包含税务查验和付款接口。 */
@Entity @Table(name = "srm_invoice")
public class InvoiceRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "statement_id") private ReconciliationStatement statement;
    @Column(nullable = false, length = 64) private String invoiceNo;
    @Column(nullable = false, precision = 16, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private LocalDate invoiceDate;
    @Column(length = 64) private String attachmentKey;
    protected InvoiceRecord() {}
    public InvoiceRecord(ReconciliationStatement statement, String invoiceNo, BigDecimal amount,
                         LocalDate invoiceDate, String attachmentKey) {
        this.statement = statement; this.invoiceNo = invoiceNo; this.amount = amount;
        this.invoiceDate = invoiceDate; this.attachmentKey = attachmentKey;
    }
    public ReconciliationStatement getStatement() { return statement; }
    public String getInvoiceNo() { return invoiceNo; }
    public BigDecimal getAmount() { return amount; }
    public LocalDate getInvoiceDate() { return invoiceDate; }
    public String getAttachmentKey() { return attachmentKey; }
}
