/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 询价定向邀请，用于供应商可见性隔离。 */
@Entity
@Table(name = "srm_sourcing_invitation", uniqueConstraints = {
    @UniqueConstraint(name = "uk_sourcing_invitation", columnNames = {"event_id", "supplier_id"})
})
public class SourcingInvitation extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private SourcingEvent event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false) private LocalDateTime invitedAt;
    private LocalDateTime viewedAt;
    protected SourcingInvitation() {}
    public SourcingInvitation(SourcingEvent event, Supplier supplier) { this.event = event; this.supplier = supplier; this.invitedAt = LocalDateTime.now(); }
    public void markViewed() { viewedAt = LocalDateTime.now(); }
    public SourcingEvent getEvent() { return event; }
    public Supplier getSupplier() { return supplier; }
    public LocalDateTime getInvitedAt() { return invitedAt; }
    public LocalDateTime getViewedAt() { return viewedAt; }
}
