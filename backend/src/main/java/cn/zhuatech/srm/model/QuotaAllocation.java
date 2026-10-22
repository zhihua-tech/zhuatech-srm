/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 单个配额方案中的供应商分配及执行余额。 */
@Entity
@Table(name = "srm_quota_allocation", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quota_plan_supplier", columnNames = {"plan_id", "supplier_id"})
})
public class QuotaAllocation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "plan_id") private QuotaPlan plan;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal quotaValue;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal allocatedQuantity;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal usedQuantity = BigDecimal.ZERO;
    @Column(length = 500) private String adjustmentNote;
    protected QuotaAllocation() {}
    public QuotaAllocation(QuotaPlan plan, Supplier supplier, BigDecimal quotaValue,
                           BigDecimal allocatedQuantity, String adjustmentNote) {
        this.plan = plan; this.supplier = supplier; this.quotaValue = quotaValue;
        this.allocatedQuantity = allocatedQuantity; this.adjustmentNote = adjustmentNote;
    }
    public void consume(BigDecimal quantity) { if (quantity.signum() <= 0) throw new IllegalArgumentException("使用数量必须大于零"); usedQuantity = usedQuantity.add(quantity); }
    public void adjustUsage(BigDecimal delta) {
        BigDecimal next = usedQuantity.add(delta);
        if (next.signum() < 0) throw new IllegalArgumentException("配额已使用数量不能小于零");
        usedQuantity = next;
    }
    public BigDecimal remainingQuantity() { return allocatedQuantity.subtract(usedQuantity); }
    public boolean isOverAllocated() { return remainingQuantity().signum() < 0; }
    public QuotaPlan getPlan() { return plan; }
    public Supplier getSupplier() { return supplier; }
    public BigDecimal getQuotaValue() { return quotaValue; }
    public BigDecimal getAllocatedQuantity() { return allocatedQuantity; }
    public BigDecimal getUsedQuantity() { return usedQuantity; }
    public String getAdjustmentNote() { return adjustmentNote; }
}
