/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 采购订单物料行，支持供应商逐行确认交期和数量。 */
@Entity
@Table(name = "srm_purchase_order_line", uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_line", columnNames = {"order_id", "line_no"})
})
public class PurchaseOrderLine extends BaseEntity {
    public enum ConfirmStatus { PENDING, CONFIRMED, PARTIAL, REJECTED }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private PurchaseOrder order;
    @Column(nullable = false) private int lineNo;
    @Column(nullable = false, length = 64) private String materialCode;
    @Column(nullable = false, length = 160) private String materialName;
    @Column(length = 300) private String specification;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal orderedQuantity;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal unitPrice;
    @Column(nullable = false, length = 16) private String unit;
    @Column(nullable = false) private LocalDate requestedDate;
    @Column(precision = 16, scale = 4) private BigDecimal confirmedQuantity;
    private LocalDate confirmedDate;
    @Column(precision = 16, scale = 4, nullable = false) private BigDecimal receivedQuantity = BigDecimal.ZERO;
    @Column(length = 500) private String supplierRemark;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private ConfirmStatus confirmStatus;
    protected PurchaseOrderLine() {}
    public PurchaseOrderLine(PurchaseOrder order, int lineNo, String materialCode, String materialName,
                             String specification, BigDecimal orderedQuantity, BigDecimal unitPrice,
                             String unit, LocalDate requestedDate) {
        this.order = order; this.lineNo = lineNo; this.materialCode = materialCode; this.materialName = materialName;
        this.specification = specification; this.orderedQuantity = orderedQuantity; this.unitPrice = unitPrice;
        this.unit = unit; this.requestedDate = requestedDate; this.confirmStatus = ConfirmStatus.PENDING;
    }
    public void confirm(BigDecimal quantity, LocalDate date, String remark) {
        if (quantity.signum() < 0 || quantity.compareTo(orderedQuantity) > 0 || quantity.compareTo(receivedQuantity) < 0) throw new IllegalArgumentException("确认数量超出可确认范围");
        confirmedQuantity = quantity; confirmedDate = date; supplierRemark = remark;
        confirmStatus = quantity.compareTo(orderedQuantity) == 0 ? ConfirmStatus.CONFIRMED : ConfirmStatus.PARTIAL;
    }
    public void revise(BigDecimal quantity, BigDecimal price, LocalDate date) {
        if (quantity.signum() <= 0 || price.signum() < 0) throw new IllegalArgumentException("订单数量和价格不合法");
        if (quantity.compareTo(receivedQuantity) < 0) throw new IllegalArgumentException("变更数量不能小于已收货数量");
        orderedQuantity = quantity;
        unitPrice = price;
        requestedDate = date;
        confirmStatus = ConfirmStatus.PENDING;
    }
    public void receive(BigDecimal quantity) {
        BigDecimal next = receivedQuantity.add(quantity);
        if (next.compareTo(orderedQuantity) > 0 || quantity.signum() < 0) throw new IllegalArgumentException("收货数量不合法");
        receivedQuantity = next;
    }
    public BigDecimal lineAmount() { return orderedQuantity.multiply(unitPrice); }
    public PurchaseOrder getOrder() { return order; }
    public int getLineNo() { return lineNo; }
    public String getMaterialCode() { return materialCode; }
    public String getMaterialName() { return materialName; }
    public String getSpecification() { return specification; }
    public BigDecimal getOrderedQuantity() { return orderedQuantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getUnit() { return unit; }
    public LocalDate getRequestedDate() { return requestedDate; }
    public BigDecimal getConfirmedQuantity() { return confirmedQuantity; }
    public LocalDate getConfirmedDate() { return confirmedDate; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public String getSupplierRemark() { return supplierRemark; }
    public ConfirmStatus getConfirmStatus() { return confirmStatus; }
}
