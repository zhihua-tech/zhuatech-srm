/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 使用真实业务数据生成采购工作台、供应商门户、消息、待办和审计查询。 */
@Service @Transactional(readOnly = true)
public class WorkspaceService {
    private final SupplierRepository suppliers; private final SourcingEventRepository sourcing;
    private final SourcingInvitationRepository invitations; private final PurchaseOrderRepository orders;
    private final QualityIssueRepository quality; private final ReconciliationRepository statements;
    private final SupplierCertificateRepository certificates; private final ContractRepository contracts;
    private final InboxItemRepository inbox; private final CurrentUserService current; private final AuditService audit;
    public WorkspaceService(SupplierRepository suppliers, SourcingEventRepository sourcing,
        SourcingInvitationRepository invitations, PurchaseOrderRepository orders, QualityIssueRepository quality,
        ReconciliationRepository statements, SupplierCertificateRepository certificates, ContractRepository contracts,
        InboxItemRepository inbox, CurrentUserService current, AuditService audit) {
        this.suppliers = suppliers; this.sourcing = sourcing; this.invitations = invitations; this.orders = orders;
        this.quality = quality; this.statements = statements; this.certificates = certificates; this.contracts = contracts;
        this.inbox = inbox; this.current = current; this.audit = audit;
    }
    public Dashboard adminDashboard() {
        String org = current.organizationCode();
        List<Supplier> supplierList = suppliers.findByOrganizationCodeAndDeletedFalse(org);
        List<SourcingEvent> eventList = sourcing.findByOrganizationCodeAndDeletedFalse(org);
        List<PurchaseOrder> orderList = orders.findByOrganizationCodeAndDeletedFalse(org, Pageable.unpaged()).getContent();
        List<QualityIssue> issueList = quality.findByOrganizationCodeAndDeletedFalse(org);
        long expiringCertificates = certificates.findByExpiresOnBetweenAndDeletedFalse(LocalDate.now(), LocalDate.now().plusDays(30)).stream()
            .filter(value -> value.getSupplier().getOrganizationCode().equals(org)).count();
        long expiringContracts = contracts.findByOrganizationCodeAndEffectiveToBetweenAndDeletedFalse(org, LocalDate.now(), LocalDate.now().plusDays(30)).size();
        List<InboxView> tasks = inbox.findByRecipientUsernameAndDeletedFalseOrderByCreatedAtDesc(current.get().getUsername()).stream().limit(10).map(InboxView::from).toList();
        return new Dashboard(List.of(
            new Metric("合作供应商", supplierList.stream().filter(value -> value.getStatus() == Supplier.Status.ACTIVE).count(), "家"),
            new Metric("进行中询价", eventList.stream().filter(value -> value.getStatus() == SourcingEvent.Status.OPEN || value.getStatus() == SourcingEvent.Status.EVALUATING).count(), "项"),
            new Metric("待确认订单", orderList.stream().filter(value -> value.getStatus() == PurchaseOrder.Status.PENDING_CONFIRMATION || value.getStatus() == PurchaseOrder.Status.PENDING_ACK).count(), "张"),
            new Metric("未关闭质量问题", issueList.stream().filter(value -> value.getStatus() != QualityIssue.Status.CLOSED).count(), "项"),
            new Metric("30 天内资质到期", expiringCertificates, "项"),
            new Metric("30 天内合同到期", expiringContracts, "份")
        ), tasks);
    }
    public Dashboard supplierDashboard() {
        Supplier supplier = current.requireSupplier();
        List<SourcingInvitation> eventList = invitations.findBySupplierIdAndDeletedFalse(supplier.getId());
        List<PurchaseOrder> orderList = orders.findBySupplierIdAndDeletedFalseOrderByDeliveryDate(supplier.getId());
        List<QualityIssue> issueList = quality.findBySupplierIdAndDeletedFalse(supplier.getId());
        List<ReconciliationStatement> statementList = statements.findBySupplierIdAndDeletedFalse(supplier.getId());
        List<InboxView> tasks = inbox.findByRecipientUsernameAndDeletedFalseOrderByCreatedAtDesc(current.get().getUsername()).stream().limit(10).map(InboxView::from).toList();
        return new Dashboard(List.of(
            new Metric("待响应询价", eventList.stream().map(SourcingInvitation::getEvent).filter(value -> value.getStatus() == SourcingEvent.Status.OPEN).count(), "项"),
            new Metric("待确认订单", orderList.stream().filter(value -> value.getStatus() == PurchaseOrder.Status.PENDING_CONFIRMATION || value.getStatus() == PurchaseOrder.Status.PENDING_ACK).count(), "张"),
            new Metric("执行中订单", orderList.stream().filter(value -> value.getStatus() != PurchaseOrder.Status.COMPLETED && value.getStatus() != PurchaseOrder.Status.CANCELLED).count(), "张"),
            new Metric("待整改质量问题", issueList.stream().filter(value -> value.getStatus() != QualityIssue.Status.CLOSED).count(), "项"),
            new Metric("待确认对账单", statementList.stream().filter(value -> value.getStatus() == ReconciliationStatement.Status.SENT).count(), "份"),
            new Metric("资质完整度", supplier.getQualificationProgress(), "%")
        ), tasks);
    }
    public List<InboxView> inbox() { return inbox.findByRecipientUsernameAndDeletedFalseOrderByCreatedAtDesc(current.get().getUsername()).stream().map(InboxView::from).toList(); }
    @Transactional public InboxView handleInbox(Long id, String action) {
        InboxItem value = inbox.findByIdAndRecipientUsernameAndDeletedFalse(id, current.get().getUsername()).orElseThrow(() -> new BusinessException("消息或待办不存在"));
        if ("READ".equalsIgnoreCase(action)) value.markRead(); else if ("COMPLETE".equalsIgnoreCase(action)) value.complete(); else throw new BusinessException("不支持的处理动作");
        return InboxView.from(value);
    }
    public Page<AuditView> audit(String actor, String module, LocalDateTime fromTime, LocalDateTime toTime, Pageable pageable) {
        if (fromTime != null && toTime != null && toTime.isBefore(fromTime)) throw new BusinessException("审计结束时间不能早于开始时间");
        return audit.search(current.organizationCode(), actor, module, fromTime, toTime, pageable).map(AuditView::from);
    }

    public record Metric(String label, long value, String unit) {}
    public record Dashboard(List<Metric> metrics, List<InboxView> workItems) {}
    public record InboxView(Long id, String kind, String status, String title, String content, String businessType,
        String businessId, String link, LocalDateTime dueAt, LocalDateTime createdAt) {
        static InboxView from(InboxItem value) { return new InboxView(value.getId(), value.getKind().name(), value.getStatus().name(), value.getTitle(), value.getContent(), value.getBusinessType(), value.getBusinessId(), value.getLink(), value.getDueAt(), value.getCreatedAt()); }
    }
    public record AuditView(Long id, String actor, String module, String action, String objectType, String objectId,
        String beforeContent, String afterContent, String clientIp, LocalDateTime occurredAt) {
        static AuditView from(AuditLog value) { return new AuditView(value.getId(), value.getActor(), value.getModule(), value.getAction(), value.getObjectType(), value.getObjectId(), value.getBeforeContent(), value.getAfterContent(), value.getClientIp(), value.getOccurredAt()); }
    }
}
