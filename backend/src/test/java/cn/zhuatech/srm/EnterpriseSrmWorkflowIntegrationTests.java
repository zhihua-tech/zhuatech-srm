/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.AuditLogRepository;
import cn.zhuatech.srm.repository.OrganizationRepository;
import cn.zhuatech.srm.repository.UserRepository;
import cn.zhuatech.srm.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** 企业级主业务闭环验收；测试数据仅写入 H2 测试库。 */
@SpringBootTest
class EnterpriseSrmWorkflowIntegrationTests {
    @Autowired private OrganizationRepository organizations;
    @Autowired private UserRepository users;
    @Autowired private AuditLogRepository auditLogs;
    @Autowired private PasswordEncoder encoder;
    @Autowired private SupplierLifecycleService supplierService;
    @Autowired private SourcingService sourcingService;
    @Autowired private ContractService contractService;
    @Autowired private QuotaService quotaService;
    @Autowired private PurchaseOrderService orderService;
    @Autowired private DeliveryQualityService deliveryQualityService;
    @Autowired private SettlementPerformanceService settlementService;
    @Autowired private BusinessRuleService businessRuleService;
    @Autowired private AuditService auditService;

    @Test
    void fullEnterpriseProcureToSettleWorkflowAndSupplierIsolation() {
        organizations.save(new Organization("ORG-T", "企业验收组织"));
        users.save(new UserAccount("acceptance.buyer", encoder.encode("Acceptance2026!"), "采购验收员",
            UserAccount.Role.ADMIN, "ORG-T", null));
        loginAs("acceptance.buyer");

        var supplierA = registerSupplier("验收供应商甲", "SUP-A", "91310000123456789A", "acceptance.supplier.a");
        var supplierB = registerSupplier("验收供应商乙", "SUP-B", "91310000123456789B", "acceptance.supplier.b");

        loginAs("acceptance.supplier.a");
        supplierService.saveQuestionnaire("{\"qualitySystem\":\"ISO9001\",\"capacity\":\"1000/month\",\"deliveryCapability\":\"全国配送\",\"complianceStatement\":\"无重大违法记录\",\"serviceCapability\":\"7x12小时响应\"}");
        var onboarding = supplierService.submitOnboarding();
        loginAs("acceptance.buyer");
        supplierService.approveOnboarding(onboarding.id(), new SupplierLifecycleService.ApprovalRequest("FIRST_APPROVE", "采购初审通过"));
        supplierService.approveOnboarding(onboarding.id(), new SupplierLifecycleService.ApprovalRequest("FINAL_APPROVE", "终审通过"));
        assertEquals(Supplier.Status.ACTIVE, supplierService.detail(supplierA.id()).supplier().status());
        loginAs("acceptance.supplier.a");
        supplierService.addCertificate(supplierA.id(), new SupplierLifecycleService.CertificateRequest(
            "ISO9001", "CERT-ACCEPT-001", LocalDate.now().minusYears(1), LocalDate.now().plusDays(10), null));
        loginAs("acceptance.buyer");
        var ruleResult = businessRuleService.refreshCurrentOrganization();
        assertTrue(ruleResult.createdReminders() >= 2, "证照提醒应同时到达采购方和供应商");
        assertTrue(ruleResult.createdRiskAlerts() >= 1, "临期证照必须形成规则风险");

        LocalDate deliveryDate = LocalDate.now().plusDays(20);
        var sourcing = sourcingService.create(new SourcingService.CreateEventRequest(
            "RFQ-ACCEPT-001", SourcingEvent.Type.RFQ, "工业控制器询价", "电子元器件", "满足企业验收规格",
            LocalDateTime.now().plusDays(5), new BigDecimal("100000"),
            List.of(new SourcingService.ItemRequest("MAT-001", "工业控制器", "ZK-100", new BigDecimal("650"), "台", deliveryDate)),
            List.of(supplierA.id(), supplierB.id()), true));

        loginAs("acceptance.supplier.a");
        var quoteA = sourcingService.submitQuote(sourcing.event().id(), new SourcingService.QuoteRequest(
            new BigDecimal("65000"), new BigDecimal("13"), 15, LocalDate.now().plusDays(10), "含税到厂", null));
        sourcingService.ask(sourcing.event().id(), new SourcingService.ClarificationRequest("包装是否需要防静电？"));
        loginAs("acceptance.supplier.b");
        sourcingService.submitQuote(sourcing.event().id(), new SourcingService.QuoteRequest(
            new BigDecimal("68000"), new BigDecimal("13"), 18, LocalDate.now().plusDays(10), "含税到厂", null));
        loginAs("acceptance.buyer");
        assertEquals(2, sourcingService.compare(sourcing.event().id()).size());
        sourcingService.evaluate(sourcing.event().id(), quoteA.id(), new SourcingService.EvaluationRequest(
            new BigDecimal("92"), new BigDecimal("90"), new BigDecimal("88"), new BigDecimal("86"), "综合评审通过"));
        sourcingService.requestAward(sourcing.event().id(), new SourcingService.AwardRequest(quoteA.id(), "综合价格与交期最优"));
        assertEquals(SourcingEvent.Status.AWARDED,
            sourcingService.approveAward(sourcing.event().id(), new SourcingService.AwardApprovalRequest(true, "审批同意")).status());

        var contract = contractService.create(new ContractService.CreateRequest(
            "CT-ACCEPT-001", supplierA.id(), "工业控制器框架合同", new BigDecimal("65000"), "CNY",
            LocalDate.now(), LocalDate.now().plusYears(1), null, "首次签订"));
        contractService.transition(contract.id(), "SUBMIT");
        contractService.transition(contract.id(), "APPROVE");
        assertEquals(ContractRecord.Status.ACTIVE, contractService.transition(contract.id(), "ACTIVATE").status());
        var contractV2 = contractService.revise(contract.id(), new ContractService.RevisionRequest(
            "工业控制器框架合同（修订）", new BigDecimal("66000"), "CNY", LocalDate.now(),
            LocalDate.now().plusYears(1), null, "调整合同限额"));
        assertEquals(2, contractV2.version());
        assertEquals(2, contractService.detail(contract.id()).versionHistory().size());

        var quota = quotaService.create(new QuotaService.CreatePlanRequest(
            "PROC-01", "FACTORY-01", "MAT-001", "工业控制器", QuotaPlan.Type.QUANTITY,
            new BigDecimal("1000"), LocalDate.now(), LocalDate.now().plusMonths(3),
            List.of(new QuotaService.AllocationRequest(supplierA.id(), new BigDecimal("600"), "主供"),
                new QuotaService.AllocationRequest(supplierB.id(), new BigDecimal("400"), "备供")), "季度配额首次发布"));
        quotaService.activate(quota.plan().id());

        var order = orderService.create(new PurchaseOrderService.OpenApiOrderRequest(
            "PO-ACCEPT-001", "SUP-A", "PROC-01", "FACTORY-01", "CNY",
            List.of(new PurchaseOrderService.OrderLineRequest(10, "MAT-001", "工业控制器", "ZK-100",
                    new BigDecimal("300"), new BigDecimal("100"), "台", deliveryDate),
                new PurchaseOrderService.OrderLineRequest(20, "MAT-001", "工业控制器", "ZK-100",
                    new BigDecimal("350"), new BigDecimal("100"), "台", deliveryDate))));
        Long firstOrderLineId = order.lines().getFirst().id();
        Long secondOrderLineId = order.lines().get(1).id();

        loginAs("acceptance.supplier.b");
        assertThrows(BusinessException.class, () -> orderService.detail(order.order().id()));
        loginAs("acceptance.supplier.a");
        var partial = orderService.confirmLine(order.order().id(), firstOrderLineId,
            new PurchaseOrderService.ConfirmLineRequest(new BigDecimal("300"), deliveryDate, "第一行按期交付"));
        assertEquals(PurchaseOrder.Status.PARTIALLY_CONFIRMED, partial.order().status());
        var confirmed = orderService.confirmLine(order.order().id(), secondOrderLineId,
            new PurchaseOrderService.ConfirmLineRequest(new BigDecimal("350"), deliveryDate, "第二行按期交付"));
        assertEquals(PurchaseOrder.Status.CONFIRMED, confirmed.order().status());
        assertTrue(confirmed.order().abnormal(), "确认数量超过分配配额时必须预警");
        var quotaAfterOrder = quotaService.detail(quota.plan().id()).allocations().stream()
            .filter(value -> value.supplierId().equals(supplierA.id())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("650").compareTo(quotaAfterOrder.usedQuantity()));
        assertTrue(quotaAfterOrder.overAllocated());
        loginAs("acceptance.buyer");
        var changed = orderService.change(order.order().id(), new PurchaseOrderService.ChangeOrderRequest(null, "调整第二行单价",
            List.of(new PurchaseOrderService.ChangeLineRequest(secondOrderLineId, new BigDecimal("350"),
                new BigDecimal("101"), deliveryDate))));
        assertEquals(2, changed.order().version());
        loginAs("acceptance.supplier.a");
        orderService.confirmOrder(order.order().id(), new PurchaseOrderService.ConfirmOrderRequest(deliveryDate, "变更版本整单确认"));
        var quotaAfterReconfirmation = quotaService.detail(quota.plan().id()).allocations().stream()
            .filter(value -> value.supplierId().equals(supplierA.id())).findFirst().orElseThrow();
        assertEquals(0, new BigDecimal("650").compareTo(quotaAfterReconfirmation.usedQuantity()), "重复确认不得重复扣减配额");

        var asn = deliveryQualityService.createAsn(new DeliveryQualityService.CreateAsnRequest(
            "ASN-ACCEPT-001", order.order().id(), LocalDateTime.now().plusDays(10),
            List.of(new DeliveryQualityService.AsnLineRequest(firstOrderLineId, new BigDecimal("300")),
                new DeliveryQualityService.AsnLineRequest(secondOrderLineId, new BigDecimal("350")))));
        deliveryQualityService.appoint(asn.asn().id(), new DeliveryQualityService.AppointmentRequest(LocalDateTime.now().plusDays(9)));
        deliveryQualityService.ship(asn.asn().id(), new DeliveryQualityService.ShipRequest("知华物流", "TRACK-001"));
        loginAs("acceptance.buyer");
        var received = deliveryQualityService.receive(asn.asn().id(), new DeliveryQualityService.ReceiveRequest(
            List.of(new DeliveryQualityService.ReceiveLineRequest(asn.lines().getFirst().id(), new BigDecimal("298"), "短少2台"),
                new DeliveryQualityService.ReceiveLineRequest(asn.lines().get(1).id(), new BigDecimal("350"), null)), "到货短少"));
        assertEquals(0, new BigDecimal("-2").compareTo(received.lines().getFirst().differenceQuantity()));

        var issue = deliveryQualityService.createQuality(new DeliveryQualityService.CreateQualityRequest(
            "NCR-ACCEPT-001", supplierA.id(), QualityIssue.Type.NCR, "外观划伤", "MAJOR", LocalDate.now().plusDays(14)));
        loginAs("acceptance.supplier.a");
        deliveryQualityService.startAnalysis(issue.id());
        deliveryQualityService.submitAction(issue.id(), new DeliveryQualityService.CorrectiveActionRequest(
            "包装隔离层不足", "补发并隔离库存", "升级包装作业指导书并双人复核", null));
        loginAs("acceptance.buyer");
        deliveryQualityService.reviewAction(issue.id(), new DeliveryQualityService.ReviewActionRequest(true, "措施可执行"));
        assertEquals(QualityIssue.Status.CLOSED,
            deliveryQualityService.close(issue.id(), new DeliveryQualityService.CloseQualityRequest("抽查三批次均合格")).status());
        assertTrue(deliveryQualityService.qualityDetail(issue.id()).history().size() >= 5);
        byte[] qualityExport = deliveryQualityService.exportQuality();
        assertTrue(qualityExport.length > 1000);
        assertEquals('P', qualityExport[0]);
        assertEquals('K', qualityExport[1]);

        var statement = settlementService.generate(new SettlementPerformanceService.GenerateStatementRequest(
            "REC-ACCEPT-001", supplierA.id(), LocalDate.now(), LocalDate.now().plusMonths(2)));
        assertEquals(2, statement.lines().size(), "对账单必须冻结订单行级明细");
        loginAs("acceptance.supplier.a");
        statement = settlementService.supplierAction(statement.id(), new SettlementPerformanceService.StatementActionRequest(true, null));
        assertEquals(ReconciliationStatement.Status.SUPPLIER_CONFIRMED, statement.status());
        settlementService.registerInvoice(statement.id(), new SettlementPerformanceService.InvoiceRequest(
            "INV-ACCEPT-001", statement.settlementAmount(), LocalDate.now(), null));
        loginAs("acceptance.buyer");
        var performance = settlementService.score(new SettlementPerformanceService.PerformanceRequest(
            supplierA.id(), LocalDate.now().minusMonths(1), LocalDate.now(), new BigDecimal("82"),
            new BigDecimal("75"), new BigDecimal("88"), new BigDecimal("80")));
        assertTrue(performance.totalScore().compareTo(BigDecimal.ZERO) > 0);
        assertTrue(auditLogs.findByOrganizationCodeOrderByOccurredAtDesc("ORG-T", Pageable.unpaged()).getTotalElements() >= 15);
        assertTrue(auditService.search("ORG-T", "acceptance", "ORDER", null, null, Pageable.unpaged()).getTotalElements() >= 3,
            "审计日志应支持操作人和模块组合筛选");
    }

    private SupplierLifecycleService.SupplierView registerSupplier(String name, String code, String registrationNo, String username) {
        var invitation = supplierService.invite(new SupplierLifecycleService.InviteRequest(name, username + "@example.invalid", "联系人", 7));
        return supplierService.register(new SupplierLifecycleService.RegisterRequest(invitation.token(), code, name,
            registrationNo, "工业控制器", "企业法人", "上海市", "联系人", "13800000000",
            username + "@example.invalid", name, "企业银行", "6222000000000000000", username, "Acceptance2026!"));
    }

    private void loginAs(String username) {
        SecurityContextHolder.getContext().setAuthentication(
            UsernamePasswordAuthenticationToken.authenticated(username, "N/A", List.of()));
    }
}
