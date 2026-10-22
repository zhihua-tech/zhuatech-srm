/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 订单与收货结果形成的基础对账单。 */
@Entity @Table(name = "srm_reconciliation")
public class ReconciliationStatement extends BaseEntity {
    public enum Status { DRAFT, SENT, SUPPLIER_CONFIRMED, DISPUTED, CLOSED }
    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, length = 40) private String statementNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false) private LocalDate periodFrom;
    @Column(nullable = false) private LocalDate periodTo;
    @Column(nullable = false, precision = 16, scale = 2) private BigDecimal orderAmount;
    @Column(nullable = false, precision = 16, scale = 2) private BigDecimal receivedAmount;
    @Column(nullable = false, precision = 16, scale = 2) private BigDecimal settlementAmount;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    @Column(length = 500) private String disputeReason;
    private LocalDateTime supplierConfirmedAt;
    protected ReconciliationStatement() {}
    public ReconciliationStatement(String organizationCode, String statementNo, Supplier supplier,
                                   LocalDate periodFrom, LocalDate periodTo, BigDecimal orderAmount,
                                   BigDecimal receivedAmount, BigDecimal settlementAmount) {
        this.organizationCode = organizationCode; this.statementNo = statementNo; this.supplier = supplier;
        this.periodFrom = periodFrom; this.periodTo = periodTo; this.orderAmount = orderAmount;
        this.receivedAmount = receivedAmount; this.settlementAmount = settlementAmount; this.status = Status.DRAFT;
    }
    public void send() { if (status != Status.DRAFT) throw new IllegalStateException("对账单不可发送"); status = Status.SENT; }
    public void confirm() { if (status != Status.SENT && status != Status.DISPUTED) throw new IllegalStateException("对账单不可确认"); status = Status.SUPPLIER_CONFIRMED; supplierConfirmedAt = LocalDateTime.now(); }
    public void dispute(String reason) { if (status != Status.SENT) throw new IllegalStateException("对账单不可异议"); disputeReason = reason; status = Status.DISPUTED; }
    public String getOrganizationCode() { return organizationCode; }
    public String getStatementNo() { return statementNo; }
    public Supplier getSupplier() { return supplier; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public BigDecimal getReceivedAmount() { return receivedAmount; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
    public Status getStatus() { return status; }
    public String getDisputeReason() { return disputeReason; }
    public LocalDateTime getSupplierConfirmedAt() { return supplierConfirmedAt; }
}
