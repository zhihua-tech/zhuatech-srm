/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 询价物料行。 */
@Entity @Table(name = "srm_sourcing_item")
public class SourcingItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private SourcingEvent event;
    @Column(nullable = false) private int lineNo;
    @Column(nullable = false, length = 64) private String materialCode;
    @Column(nullable = false, length = 160) private String materialName;
    @Column(length = 300) private String specification;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal quantity;
    @Column(nullable = false, length = 16) private String unit;
    @Column(nullable = false) private LocalDate requiredDate;
    protected SourcingItem() {}
    public SourcingItem(SourcingEvent event, int lineNo, String materialCode, String materialName,
                        String specification, BigDecimal quantity, String unit, LocalDate requiredDate) {
        this.event = event; this.lineNo = lineNo; this.materialCode = materialCode; this.materialName = materialName;
        this.specification = specification; this.quantity = quantity; this.unit = unit; this.requiredDate = requiredDate;
    }
    public SourcingEvent getEvent() { return event; }
    public int getLineNo() { return lineNo; }
    public String getMaterialCode() { return materialCode; }
    public String getMaterialName() { return materialName; }
    public String getSpecification() { return specification; }
    public BigDecimal getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public LocalDate getRequiredDate() { return requiredDate; }
}
