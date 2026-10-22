/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** ASN 明细及收货差异。 */
@Entity @Table(name = "srm_asn_line")
public class AsnLine extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "asn_id") private AdvanceShippingNotice asn;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_line_id") private PurchaseOrderLine orderLine;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal shippedQuantity;
    @Column(nullable = false, precision = 16, scale = 4) private BigDecimal receivedQuantity = BigDecimal.ZERO;
    @Column(length = 500) private String discrepancyReason;
    protected AsnLine() {}
    public AsnLine(AdvanceShippingNotice asn, PurchaseOrderLine orderLine, BigDecimal shippedQuantity) {
        this.asn = asn; this.orderLine = orderLine; this.shippedQuantity = shippedQuantity;
    }
    public void receive(BigDecimal quantity, String reason) { if (quantity.signum() < 0 || quantity.compareTo(shippedQuantity) > 0) throw new IllegalArgumentException("实收数量不合法"); receivedQuantity = quantity; discrepancyReason = reason; }
    public BigDecimal differenceQuantity() { return receivedQuantity.subtract(shippedQuantity); }
    public AdvanceShippingNotice getAsn() { return asn; }
    public PurchaseOrderLine getOrderLine() { return orderLine; }
    public BigDecimal getShippedQuantity() { return shippedQuantity; }
    public BigDecimal getReceivedQuantity() { return receivedQuantity; }
    public String getDiscrepancyReason() { return discrepancyReason; }
}
