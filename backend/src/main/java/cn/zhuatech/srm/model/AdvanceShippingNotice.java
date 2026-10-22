/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** ASN 送货通知、预约和到货状态。 */
@Entity
@Table(name = "srm_asn", indexes = {@Index(name = "idx_asn_org_supplier_status", columnList = "organization_code,supplier_id,status")})
public class AdvanceShippingNotice extends BaseEntity {
    public enum Status { DRAFT, APPOINTED, SHIPPED, PARTIALLY_RECEIVED, RECEIVED, CANCELLED }
    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, length = 40) private String asnNo;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "order_id") private PurchaseOrder order;
    @Column(nullable = false) private LocalDateTime plannedArrivalAt;
    private LocalDateTime appointmentAt;
    private LocalDateTime shippedAt;
    private LocalDateTime receivedAt;
    @Column(length = 80) private String carrierName;
    @Column(length = 80) private String trackingNo;
    @Column(length = 500) private String discrepancyReason;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 24) private Status status;
    protected AdvanceShippingNotice() {}
    public AdvanceShippingNotice(String organizationCode, String asnNo, Supplier supplier,
                                 PurchaseOrder order, LocalDateTime plannedArrivalAt) {
        this.organizationCode = organizationCode; this.asnNo = asnNo; this.supplier = supplier;
        this.order = order; this.plannedArrivalAt = plannedArrivalAt; this.status = Status.DRAFT;
    }
    public void appoint(LocalDateTime value) { if (status != Status.DRAFT) throw new IllegalStateException("仅草稿可预约"); appointmentAt = value; status = Status.APPOINTED; }
    public void ship(String carrier, String trackingNo) { if (status != Status.APPOINTED && status != Status.DRAFT) throw new IllegalStateException("ASN 当前状态不可发货"); carrierName = carrier; this.trackingNo = trackingNo; shippedAt = LocalDateTime.now(); status = Status.SHIPPED; }
    public void receive(boolean complete, String discrepancyReason) { if (status != Status.SHIPPED && status != Status.PARTIALLY_RECEIVED) throw new IllegalStateException("ASN 尚未发货"); this.discrepancyReason = discrepancyReason; status = complete ? Status.RECEIVED : Status.PARTIALLY_RECEIVED; if (complete) receivedAt = LocalDateTime.now(); }
    public String getOrganizationCode() { return organizationCode; }
    public String getAsnNo() { return asnNo; }
    public Supplier getSupplier() { return supplier; }
    public PurchaseOrder getOrder() { return order; }
    public LocalDateTime getPlannedArrivalAt() { return plannedArrivalAt; }
    public LocalDateTime getAppointmentAt() { return appointmentAt; }
    public LocalDateTime getShippedAt() { return shippedAt; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public String getCarrierName() { return carrierName; }
    public String getTrackingNo() { return trackingNo; }
    public String getDiscrepancyReason() { return discrepancyReason; }
    public Status getStatus() { return status; }
}
