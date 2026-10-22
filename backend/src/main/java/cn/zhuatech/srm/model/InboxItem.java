/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 采购工作台和供应商门户共用的站内消息及业务待办。 */
@Entity
@Table(name = "srm_inbox_item", indexes = {
    @Index(name = "idx_inbox_recipient_status", columnList = "recipient_username,status,due_at")
})
public class InboxItem extends BaseEntity {
    public enum Kind { MESSAGE, TASK, ALERT }
    public enum Status { OPEN, COMPLETED, READ, CANCELLED }

    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, length = 64) private String recipientUsername;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Kind kind;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 16) private Status status;
    @Column(nullable = false, length = 160) private String title;
    @Column(length = 1000) private String content;
    @Column(length = 80) private String businessType;
    @Column(length = 80) private String businessId;
    @Column(length = 300) private String link;
    private LocalDateTime dueAt;
    private LocalDateTime handledAt;

    protected InboxItem() {}

    public InboxItem(String organizationCode, String recipientUsername, Supplier supplier, Kind kind,
                     String title, String content, String businessType, String businessId,
                     String link, LocalDateTime dueAt) {
        this.organizationCode = organizationCode;
        this.recipientUsername = recipientUsername;
        this.supplier = supplier;
        this.kind = kind;
        this.title = title;
        this.content = content;
        this.businessType = businessType;
        this.businessId = businessId;
        this.link = link;
        this.dueAt = dueAt;
        this.status = Status.OPEN;
    }

    public void complete() { status = Status.COMPLETED; handledAt = LocalDateTime.now(); }
    public void markRead() { if (status == Status.OPEN && kind == Kind.MESSAGE) status = Status.READ; }
    public String getOrganizationCode() { return organizationCode; }
    public String getRecipientUsername() { return recipientUsername; }
    public Supplier getSupplier() { return supplier; }
    public Kind getKind() { return kind; }
    public Status getStatus() { return status; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public String getBusinessType() { return businessType; }
    public String getBusinessId() { return businessId; }
    public String getLink() { return link; }
    public LocalDateTime getDueAt() { return dueAt; }
    public LocalDateTime getHandledAt() { return handledAt; }
}
