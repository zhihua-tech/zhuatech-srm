/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;

/** 供应商关键资料和合作状态变更记录。 */
@Entity
@Table(name = "srm_supplier_change_record")
public class SupplierChangeRecord extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 64) private String changedBy;
    @Column(nullable = false, length = 60) private String fieldName;
    @Column(columnDefinition = "TEXT") private String beforeValue;
    @Column(columnDefinition = "TEXT") private String afterValue;
    @Column(length = 500) private String reason;
    protected SupplierChangeRecord() {}
    public SupplierChangeRecord(Supplier supplier, String changedBy, String fieldName,
                                String beforeValue, String afterValue, String reason) {
        this.supplier = supplier; this.changedBy = changedBy; this.fieldName = fieldName;
        this.beforeValue = beforeValue; this.afterValue = afterValue; this.reason = reason;
    }
    public Supplier getSupplier() { return supplier; }
    public String getChangedBy() { return changedBy; }
    public String getFieldName() { return fieldName; }
    public String getBeforeValue() { return beforeValue; }
    public String getAfterValue() { return afterValue; }
    public String getReason() { return reason; }
}
