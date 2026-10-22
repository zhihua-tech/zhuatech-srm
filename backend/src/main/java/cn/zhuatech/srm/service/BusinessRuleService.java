/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.List;

/** 将合同、资质、订单和质量规则转成可追踪待办与风险预警。 */
@Service
public class BusinessRuleService {
    private final ContractRepository contracts;
    private final SupplierCertificateRepository certificates;
    private final PurchaseOrderRepository orders;
    private final QualityIssueRepository qualityIssues;
    private final UserRepository users;
    private final InboxItemRepository inbox;
    private final RiskAlertRepository risks;
    private final CurrentUserService current;
    private final AuditService audit;

    public BusinessRuleService(ContractRepository contracts, SupplierCertificateRepository certificates,
                               PurchaseOrderRepository orders, QualityIssueRepository qualityIssues,
                               UserRepository users, InboxItemRepository inbox, RiskAlertRepository risks,
                               CurrentUserService current, AuditService audit) {
        this.contracts = contracts; this.certificates = certificates; this.orders = orders;
        this.qualityIssues = qualityIssues; this.users = users; this.inbox = inbox;
        this.risks = risks; this.current = current; this.audit = audit;
    }

    /** 每天凌晨执行；所有写入均以业务来源幂等，不会重复制造待办。 */
    @Scheduled(cron = "${app.business-rules.cron:0 15 1 * * *}")
    @Transactional
    public void refreshAll() {
        users.findAll().stream().filter(user -> !user.isDeleted()).map(UserAccount::getOrganizationCode).distinct()
            .forEach(this::refreshOrganization);
    }

    @Transactional
    public RefreshResult refreshCurrentOrganization() { return refreshOrganization(current.organizationCode()); }

    private RefreshResult refreshOrganization(String organizationCode) {
        int reminders = 0;
        int alerts = 0;
        LocalDate today = LocalDate.now();

        for (ContractRecord contract : contracts.findByOrganizationCodeAndDeletedFalse(organizationCode)) {
            if (contract.getStatus() == ContractRecord.Status.ACTIVE && contract.getEffectiveTo().isBefore(today)) {
                contract.expire();
                audit.record(organizationCode, "SYSTEM", "CONTRACT", "AUTO_EXPIRE", "CONTRACT", contract.getId(),
                    ContractRecord.Status.ACTIVE, ContractRecord.Status.EXPIRED);
            }
            if (contract.getStatus() == ContractRecord.Status.ACTIVE && !contract.getEffectiveTo().isAfter(today.plusDays(30))) {
                reminders += notifyRoles(organizationCode, List.of(UserAccount.Role.ADMIN, UserAccount.Role.BUYER), null,
                    InboxItem.Kind.ALERT, "合同即将到期：" + contract.getContractNo(), "合同有效期截止 " + contract.getEffectiveTo(),
                    "CONTRACT", String.valueOf(contract.getId()), "/admin/contracts", contract.getEffectiveTo().atTime(9, 0));
                BigDecimal executed = orders.findBySupplierIdAndDeliveryDateBetweenAndDeletedFalse(contract.getSupplier().getId(),
                        contract.getEffectiveFrom(), contract.getEffectiveTo()).stream()
                    .filter(order -> order.getStatus() != PurchaseOrder.Status.CANCELLED)
                    .map(PurchaseOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal rate = contract.getAmount().signum() == 0 ? BigDecimal.ZERO
                    : executed.multiply(new BigDecimal("100")).divide(contract.getAmount(), 2, RoundingMode.HALF_UP);
                if (rate.compareTo(new BigDecimal("80")) < 0) {
                    reminders += notifyRoles(organizationCode, List.of(UserAccount.Role.ADMIN, UserAccount.Role.BUYER),
                        contract.getSupplier(), InboxItem.Kind.TASK, "合同履约进度偏低：" + contract.getContractNo(),
                        "合同将在 30 天内到期，当前订单执行金额 " + executed + "，履约率 " + rate + "%",
                        "CONTRACT_FULFILLMENT", String.valueOf(contract.getId()), "/admin/contracts", contract.getEffectiveTo().atTime(9, 0));
                }
            }
        }

        for (SupplierCertificate certificate : certificates.findByExpiresOnBetweenAndDeletedFalse(today.minusYears(20), today.plusDays(30))) {
            Supplier supplier = certificate.getSupplier();
            if (!organizationCode.equals(supplier.getOrganizationCode())) continue;
            boolean expired = certificate.getExpiresOn().isBefore(today);
            String title = (expired ? "资质已过期：" : "资质即将到期：") + certificate.getCertificateType();
            String content = supplier.getSupplierName() + "，证照编号 " + certificate.getCertificateNo() + "，有效期至 " + certificate.getExpiresOn();
            reminders += notifyRoles(organizationCode, List.of(UserAccount.Role.ADMIN, UserAccount.Role.BUYER), supplier,
                InboxItem.Kind.ALERT, title, content, "CERTIFICATE", String.valueOf(certificate.getId()), "/admin/suppliers", certificate.getExpiresOn().atTime(9, 0));
            reminders += notifyUsers(users.findBySupplierIdAndEnabledTrue(supplier.getId()), supplier, InboxItem.Kind.ALERT,
                title, content, "CERTIFICATE", String.valueOf(certificate.getId()), "/portal/profile", certificate.getExpiresOn().atTime(9, 0));
            alerts += ensureRisk(organizationCode, supplier, "CERTIFICATE_EXPIRY", "CERTIFICATE", certificate.getId(),
                expired ? "HIGH" : "MEDIUM", content, certificate.getExpiresOn().atTime(18, 0));
        }

        for (PurchaseOrder order : orders.findByDeletedFalse()) {
            if (!organizationCode.equals(order.getOrganizationCode())) continue;
            boolean pendingConfirmation = order.getStatus() == PurchaseOrder.Status.PENDING_ACK
                || order.getStatus() == PurchaseOrder.Status.PENDING_CONFIRMATION
                || order.getStatus() == PurchaseOrder.Status.PARTIALLY_CONFIRMED;
            if (pendingConfirmation && order.getCreatedAt().isBefore(LocalDateTime.now().minusDays(2))) {
                String content = "采购订单 " + order.getOrderNo() + " 发出超过 2 天仍未完成供应商确认";
                reminders += notifyRoles(organizationCode, List.of(UserAccount.Role.ADMIN, UserAccount.Role.BUYER), order.getSupplier(), InboxItem.Kind.ALERT,
                    "采购订单未确认", content, "PURCHASE_ORDER_CONFIRMATION", String.valueOf(order.getId()), "/admin/orders", LocalDateTime.now());
                reminders += notifyUsers(users.findBySupplierIdAndEnabledTrue(order.getSupplier().getId()), order.getSupplier(), InboxItem.Kind.TASK,
                    "采购订单待确认", content, "PURCHASE_ORDER_CONFIRMATION", String.valueOf(order.getId()), "/portal/orders", LocalDateTime.now());
            }
            if (!order.getDeliveryDate().isBefore(today) || order.getStatus() == PurchaseOrder.Status.COMPLETED
                || order.getStatus() == PurchaseOrder.Status.CANCELLED) continue;
            String content = "采购订单 " + order.getOrderNo() + " 已超过交期 " + order.getDeliveryDate() + "，当前完成率 " + order.getDeliveredPercent() + "%";
            reminders += notifyRoles(organizationCode, List.of(UserAccount.Role.ADMIN, UserAccount.Role.BUYER), order.getSupplier(), InboxItem.Kind.ALERT,
                "采购订单逾期", content, "PURCHASE_ORDER", String.valueOf(order.getId()), "/admin/orders", LocalDateTime.now());
            reminders += notifyUsers(users.findBySupplierIdAndEnabledTrue(order.getSupplier().getId()), order.getSupplier(), InboxItem.Kind.TASK,
                "采购订单逾期待处理", content, "PURCHASE_ORDER", String.valueOf(order.getId()), "/portal/orders", LocalDateTime.now());
            alerts += ensureRisk(organizationCode, order.getSupplier(), "ORDER_OVERDUE", "PURCHASE_ORDER", order.getId(), "HIGH", content, LocalDateTime.now().plusDays(3));
        }

        for (QualityIssue issue : qualityIssues.findByOrganizationCodeAndDeletedFalse(organizationCode)) {
            if (issue.getStatus() == QualityIssue.Status.CLOSED || !issue.getDueDate().isBefore(today)) continue;
            String content = "质量问题 " + issue.getIssueNo() + " 已超过整改期限 " + issue.getDueDate();
            reminders += notifyRoles(organizationCode, List.of(UserAccount.Role.ADMIN, UserAccount.Role.QUALITY), issue.getSupplier(), InboxItem.Kind.ALERT,
                "质量整改逾期", content, "QUALITY_ISSUE", String.valueOf(issue.getId()), "/admin/quality", LocalDateTime.now());
            reminders += notifyUsers(users.findBySupplierIdAndEnabledTrue(issue.getSupplier().getId()), issue.getSupplier(), InboxItem.Kind.TASK,
                "质量整改逾期待提交", content, "QUALITY_ISSUE", String.valueOf(issue.getId()), "/portal/quality", LocalDateTime.now());
            alerts += ensureRisk(organizationCode, issue.getSupplier(), "QUALITY_ACTION_OVERDUE", "QUALITY_ISSUE", issue.getId(), "HIGH", content, LocalDateTime.now().plusDays(3));
        }
        return new RefreshResult(reminders, alerts);
    }

    private int notifyRoles(String organizationCode, Collection<UserAccount.Role> roles, Supplier supplier,
                            InboxItem.Kind kind, String title, String content, String businessType,
                            String businessId, String link, LocalDateTime dueAt) {
        return roles.stream().flatMap(role -> users.findByOrganizationCodeAndRoleAndEnabledTrue(organizationCode, role).stream())
            .mapToInt(user -> ensureInbox(user, supplier, kind, title, content, businessType, businessId, link, dueAt)).sum();
    }

    private int notifyUsers(List<UserAccount> recipients, Supplier supplier, InboxItem.Kind kind, String title,
                            String content, String businessType, String businessId, String link, LocalDateTime dueAt) {
        return recipients.stream().mapToInt(user -> ensureInbox(user, supplier, kind, title, content, businessType, businessId, link, dueAt)).sum();
    }

    private int ensureInbox(UserAccount user, Supplier supplier, InboxItem.Kind kind, String title, String content,
                            String businessType, String businessId, String link, LocalDateTime dueAt) {
        if (inbox.existsByRecipientUsernameAndBusinessTypeAndBusinessIdAndTitleAndDeletedFalse(user.getUsername(), businessType, businessId, title)) return 0;
        inbox.save(new InboxItem(user.getOrganizationCode(), user.getUsername(), supplier, kind, title, content, businessType, businessId, link, dueAt));
        return 1;
    }

    private int ensureRisk(String organizationCode, Supplier supplier, String ruleCode, String sourceType,
                           Object sourceId, String level, String description, LocalDateTime dueAt) {
        String id = String.valueOf(sourceId);
        if (risks.existsByOrganizationCodeAndSupplierIdAndRuleCodeAndSourceIdAndStatusNotAndDeletedFalse(
            organizationCode, supplier.getId(), ruleCode, id, RiskAlert.Status.CLOSED)) return 0;
        risks.save(new RiskAlert(organizationCode, supplier, ruleCode, sourceType, id, level, description, null, dueAt));
        return 1;
    }

    public record RefreshResult(int createdReminders, int createdRiskAlerts) {}
}
