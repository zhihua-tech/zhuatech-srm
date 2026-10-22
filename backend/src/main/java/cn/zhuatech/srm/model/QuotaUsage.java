/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** 配额使用明细，来源可以是采购订单或经授权的手工调整。 */
@Entity
@Table(name = "srm_quota_usage", uniqueConstraints = {
    @UniqueConstraint(name = "uk_quota_usage_source", columnNames = {"allocation_id", "source_type", "source_id"})
})
public class QuotaUsage extends BaseEntity {
    public enum SourceType { PURCHASE_ORDER, MANUAL }
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "allocation_id") private QuotaAllocation allocation;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private SourceType sourceType;
    @Column(nullable = false, length = 80) private String sourceId;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal quantity;
    @Column(nullable = false, length = 64) private String operatedBy;
    @Column(length = 500) private String remark;
    protected QuotaUsage() {}
    public QuotaUsage(QuotaAllocation allocation, SourceType sourceType, String sourceId,
                      BigDecimal quantity, String operatedBy, String remark) {
        this.allocation = allocation; this.sourceType = sourceType; this.sourceId = sourceId;
        this.quantity = quantity; this.operatedBy = operatedBy; this.remark = remark;
    }
    public void adjust(BigDecimal newQuantity, String actor, String remark) {
        if (newQuantity.signum() < 0) throw new IllegalArgumentException("配额使用数量不能小于零");
        this.quantity = newQuantity;
        this.operatedBy = actor;
        this.remark = remark;
    }
    public QuotaAllocation getAllocation() { return allocation; }
    public SourceType getSourceType() { return sourceType; }
    public String getSourceId() { return sourceId; }
    public BigDecimal getQuantity() { return quantity; }
    public String getOperatedBy() { return operatedBy; }
    public String getRemark() { return remark; }
}
