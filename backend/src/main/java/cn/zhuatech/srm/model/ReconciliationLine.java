/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 对账单中的订单行、收货数量与结算金额快照。 */
@Entity
@Table(name = "srm_reconciliation_line", indexes = {
    @Index(name = "idx_reconciliation_line_statement", columnList = "statement_id,line_no")
})
public class ReconciliationLine extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "statement_id")
    private ReconciliationStatement statement;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private PurchaseOrder order;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_id")
    private PurchaseOrderLine orderLine;
    @Column(nullable = false)
    private int lineNo;
    @Column(nullable = false, length = 64)
    private String materialCode;
    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal orderedQuantity;
    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal receivedQuantity;
    @Column(nullable = false, precision = 16, scale = 4)
    private BigDecimal unitPrice;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal orderAmount;
    @Column(nullable = false, precision = 16, scale = 2)
    private BigDecimal settlementAmount;

    protected ReconciliationLine() {}

    public ReconciliationLine(ReconciliationStatement statement, PurchaseOrder order, PurchaseOrderLine orderLine) {
        this.statement = statement;
        this.order = order;
        this.orderLine = orderLine;
        this.lineNo = orderLine.getLineNo();
        this.materialCode = orderLine.getMaterialCode();
        this.orderedQuantity = orderLine.getOrderedQuantity();
        this.receivedQuantity = orderLine.getReceivedQuantity();
        this.unitPrice = orderLine.getUnitPrice();
        this.orderAmount = orderLine.getOrderedQuantity().multiply(orderLine.getUnitPrice());
        this.settlementAmount = orderLine.getReceivedQuantity().multiply(orderLine.getUnitPrice());
    }

    public ReconciliationStatement getStatement() { return statement; }
    public PurchaseOrder getOrder() { return order; }
    public PurchaseOrderLine getOrderLine() { return orderLine; }
    public int getLineNo() { return lineNo; }
    public String getMaterialCode() { return materialCode; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public BigDecimal getSettlementAmount() { return settlementAmount; }
}
