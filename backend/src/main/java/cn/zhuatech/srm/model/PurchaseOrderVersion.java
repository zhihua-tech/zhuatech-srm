/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;

/** 采购订单变更前的不可覆盖快照。 */
@Entity @Table(name = "srm_purchase_order_version")
public class PurchaseOrderVersion extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private PurchaseOrder order;
    @Column(nullable = false) private int orderVersion;
    @Column(nullable = false, columnDefinition = "TEXT") private String snapshotJson;
    @Column(nullable = false, length = 500) private String changeReason;
    @Column(nullable = false, length = 64) private String changedBy;
    protected PurchaseOrderVersion() {}
    public PurchaseOrderVersion(PurchaseOrder order, int orderVersion, String snapshotJson, String changeReason, String changedBy) {
        this.order = order; this.orderVersion = orderVersion; this.snapshotJson = snapshotJson;
        this.changeReason = changeReason; this.changedBy = changedBy;
    }
    public PurchaseOrder getOrder() { return order; }
    public int getOrderVersion() { return orderVersion; }
    public String getSnapshotJson() { return snapshotJson; }
    public String getChangeReason() { return changeReason; }
    public String getChangedBy() { return changedBy; }
}
