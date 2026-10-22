/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/** 不可变操作审计日志。 */
@Entity
@Table(name = "srm_audit_log")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 32) private String organizationCode;
    @Column(nullable = false, length = 64) private String actor;
    @Column(nullable = false, length = 40) private String module;
    @Column(nullable = false, length = 40) private String action;
    @Column(nullable = false, length = 60) private String objectType;
    @Column(length = 80) private String objectId;
    @Column(columnDefinition = "TEXT") private String beforeContent;
    @Column(columnDefinition = "TEXT") private String afterContent;
    @Column(length = 64) private String clientIp;
    @Column(nullable = false, updatable = false) private LocalDateTime occurredAt;

    protected AuditLog() {}

    public AuditLog(String organizationCode, String actor, String module, String action,
                    String objectType, String objectId, String beforeContent, String afterContent,
                    String clientIp) {
        this.organizationCode = organizationCode;
        this.actor = actor;
        this.module = module;
        this.action = action;
        this.objectType = objectType;
        this.objectId = objectId;
        this.beforeContent = beforeContent;
        this.afterContent = afterContent;
        this.clientIp = clientIp;
        this.occurredAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getOrganizationCode() { return organizationCode; }
    public String getActor() { return actor; }
    public String getModule() { return module; }
    public String getAction() { return action; }
    public String getObjectType() { return objectType; }
    public String getObjectId() { return objectId; }
    public String getBeforeContent() { return beforeContent; }
    public String getAfterContent() { return afterContent; }
    public String getClientIp() { return clientIp; }
    public LocalDateTime getOccurredAt() { return occurredAt; }
}
