/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 采购订单头及版本状态。 */
@Entity
@Table(name = "srm_purchase_order", uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_org_no", columnNames = {"organization_code", "order_no"})
}, indexes = {
    @Index(name = "idx_order_org_supplier_status", columnList = "organization_code,supplier_id,status")
})
public class PurchaseOrder extends BaseEntity {
    public enum Status {
        DRAFT, PENDING_CONFIRMATION, PARTIALLY_CONFIRMED, CONFIRMED, SHIPPING,
        PARTIALLY_RECEIVED, COMPLETED, CANCELLED,
        PENDING_ACK, PRODUCING, PART_DELIVERED
    }

    @Column(nullable = false, length = 32) private String organizationCode = "DEFAULT";
    @Column(nullable = false, length = 32) private String orderNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 32) private String procurementOrganization = "DEFAULT";
    @Column(nullable = false, length = 32) private String factoryCode = "DEFAULT";
    @Column(nullable = false, length = 150) private String itemSummary;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 8) private String currency = "CNY";
    @Column(nullable = false) private LocalDate deliveryDate;
    @Column(nullable = false) private int deliveredPercent;
    @Column(nullable = false) private int orderVersion = 1;
    @Column(length = 500) private String changeReason;
    @Column(nullable = false) private boolean abnormal;
    @Column(length = 500) private String abnormalReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32) private Status status;
    private LocalDateTime confirmedAt;

    protected PurchaseOrder() {}

    public PurchaseOrder(String organizationCode, String orderNo, Supplier supplier, String procurementOrganization,
                         String factoryCode, String itemSummary, BigDecimal amount, String currency,
                         LocalDate deliveryDate) {
        this.organizationCode = organizationCode; this.orderNo = orderNo; this.supplier = supplier;
        this.procurementOrganization = procurementOrganization; this.factoryCode = factoryCode;
        this.itemSummary = itemSummary; this.amount = amount; this.currency = currency;
        this.deliveryDate = deliveryDate; this.status = Status.PENDING_CONFIRMATION;
    }

    /** 兼容旧版扩展代码。 */
    public PurchaseOrder(String no, Supplier supplier, String items, BigDecimal amount, LocalDate date,
                         int percent, Status status) {
        this("DEFAULT", no, supplier, "DEFAULT", "DEFAULT", items, amount, "CNY", date);
        this.deliveredPercent = percent; this.status = status;
    }

    public void updateStatus(Status target) { this.status = target; }
    public void acknowledge() {
        if (status != Status.PENDING_ACK && status != Status.PENDING_CONFIRMATION && status != Status.PARTIALLY_CONFIRMED) {
            throw new IllegalStateException("订单当前状态不可确认");
        }
        status = Status.CONFIRMED; confirmedAt = LocalDateTime.now();
    }
    public void markPartiallyConfirmed() { status = Status.PARTIALLY_CONFIRMED; }
    public void createVersion(String reason) { orderVersion++; changeReason = reason; status = Status.PENDING_CONFIRMATION; confirmedAt = null; }
    public void changeDeliveryDate(LocalDate value) { deliveryDate = value; }
    public void recalculate(BigDecimal value, LocalDate latestDate) { amount = value; deliveryDate = latestDate; }
    public void markAbnormal(String reason) { abnormal = true; abnormalReason = reason; }
    public void clearAbnormal() { abnormal = false; abnormalReason = null; }
    public void updateReceiptPercent(int percent) { deliveredPercent = Math.max(0, Math.min(100, percent)); status = deliveredPercent >= 100 ? Status.COMPLETED : deliveredPercent > 0 ? Status.PARTIALLY_RECEIVED : status; }

    public String getOrganizationCode() { return organizationCode; }
    public String getOrderNo() { return orderNo; }
    public Supplier getSupplier() { return supplier; }
    public String getProcurementOrganization() { return procurementOrganization; }
    public String getFactoryCode() { return factoryCode; }
    public String getItemSummary() { return itemSummary; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getDeliveryDate() { return deliveryDate; }
    public int getDeliveredPercent() { return deliveredPercent; }
    public int getOrderVersion() { return orderVersion; }
    public String getChangeReason() { return changeReason; }
    public boolean isAbnormal() { return abnormal; }
    public String getAbnormalReason() { return abnormalReason; }
    public Status getStatus() { return status; }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
}
