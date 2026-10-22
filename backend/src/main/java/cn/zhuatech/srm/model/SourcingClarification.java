/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** 询价澄清问答。 */
@Entity @Table(name = "srm_sourcing_clarification")
public class SourcingClarification extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "event_id") private SourcingEvent event;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 1000) private String question;
    @Column(length = 2000) private String answer;
    @Column(nullable = false, length = 64) private String askedBy;
    @Column(length = 64) private String answeredBy;
    private LocalDateTime answeredAt;
    protected SourcingClarification() {}
    public SourcingClarification(SourcingEvent event, Supplier supplier, String question, String askedBy) {
        this.event = event; this.supplier = supplier; this.question = question; this.askedBy = askedBy;
    }
    public void answer(String value, String actor) { if (answer != null) throw new IllegalStateException("澄清已回复"); answer = value; answeredBy = actor; answeredAt = LocalDateTime.now(); }
    public SourcingEvent getEvent() { return event; }
    public Supplier getSupplier() { return supplier; }
    public String getQuestion() { return question; }
    public String getAnswer() { return answer; }
    public String getAskedBy() { return askedBy; }
    public String getAnsweredBy() { return answeredBy; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
}
