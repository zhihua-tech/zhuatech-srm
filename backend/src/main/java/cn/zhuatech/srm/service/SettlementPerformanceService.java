/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** 基础对账、发票登记、供应商绩效和规则型风险整改。 */
@Service @Transactional(readOnly = true)
public class SettlementPerformanceService {
    private final ReconciliationRepository statements; private final ReconciliationLineRepository statementLines;
    private final InvoiceRepository invoices;
    private final PurchaseOrderRepository orders; private final PurchaseOrderLineRepository lines;
    private final SupplierRepository suppliers; private final SupplierPerformanceRepository performance;
    private final RiskAlertRepository risks; private final CurrentUserService current; private final AuditService audit;
    public SettlementPerformanceService(ReconciliationRepository statements, ReconciliationLineRepository statementLines,
        InvoiceRepository invoices,
        PurchaseOrderRepository orders, PurchaseOrderLineRepository lines, SupplierRepository suppliers,
        SupplierPerformanceRepository performance, RiskAlertRepository risks, CurrentUserService current, AuditService audit) {
        this.statements = statements; this.statementLines = statementLines; this.invoices = invoices;
        this.orders = orders; this.lines = lines;
        this.suppliers = suppliers; this.performance = performance; this.risks = risks; this.current = current; this.audit = audit;
    }
    public List<StatementView> statements() { return (current.isSupplier() ? statements.findBySupplierIdAndDeletedFalse(current.requireSupplier().getId()) : statements.findByOrganizationCodeAndDeletedFalse(current.organizationCode())).stream().map(this::statementView).toList(); }
    public StatementView detail(Long id) { return statementView(scopedStatement(id)); }
    @Transactional public StatementView generate(GenerateStatementRequest request) {
        if (request.periodTo().isBefore(request.periodFrom())) throw new BusinessException("对账结束日期不能早于开始日期");
        if (statements.existsByOrganizationCodeAndStatementNoAndDeletedFalse(current.organizationCode(), request.statementNo())) throw new BusinessException("对账单号已存在");
        Supplier supplier = suppliers.findByIdAndOrganizationCodeAndDeletedFalse(request.supplierId(), current.organizationCode()).orElseThrow(() -> new BusinessException("供应商不存在"));
        List<PurchaseOrder> selected = orders.findBySupplierIdAndDeletedFalseOrderByDeliveryDate(supplier.getId()).stream()
            .filter(order -> !order.getDeliveryDate().isBefore(request.periodFrom()) && !order.getDeliveryDate().isAfter(request.periodTo())).toList();
        if (selected.isEmpty()) throw new BusinessException("对账期间内没有可对账采购订单");
        BigDecimal orderAmount = selected.stream().map(PurchaseOrder::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal receivedAmount = selected.stream().flatMap(order -> lines.findByOrderIdAndDeletedFalseOrderByLineNo(order.getId()).stream())
            .map(line -> line.getReceivedQuantity().multiply(line.getUnitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        ReconciliationStatement statement = statements.save(new ReconciliationStatement(current.organizationCode(), request.statementNo(), supplier,
            request.periodFrom(), request.periodTo(), orderAmount, receivedAmount, receivedAmount));
        selected.forEach(order -> lines.findByOrderIdAndDeletedFalseOrderByLineNo(order.getId())
            .forEach(line -> statementLines.save(new ReconciliationLine(statement, order, line))));
        statement.send();
        audit.record(current.organizationCode(), current.get().getUsername(), "RECONCILIATION", "GENERATE", "STATEMENT", statement.getId(), null,
            Map.of("orderAmount", orderAmount, "receivedAmount", receivedAmount));
        return statementView(statement);
    }
    @Transactional public StatementView supplierAction(Long id, StatementActionRequest request) {
        ReconciliationStatement statement = statements.findByIdAndSupplierIdAndDeletedFalse(id, current.requireSupplier().getId()).orElseThrow(() -> new BusinessException("无权访问该对账单"));
        if (!request.confirm() && (request.reason() == null || request.reason().isBlank())) throw new BusinessException("提出对账异议时必须填写原因");
        if (request.confirm()) statement.confirm(); else statement.dispute(request.reason());
        audit.record(current.organizationCode(), current.get().getUsername(), "RECONCILIATION", request.confirm() ? "CONFIRM" : "DISPUTE", "STATEMENT", id, null, request);
        return statementView(statement);
    }
    @Transactional public InvoiceView registerInvoice(Long statementId, InvoiceRequest request) {
        ReconciliationStatement statement = statements.findByIdAndSupplierIdAndDeletedFalse(statementId, current.requireSupplier().getId()).orElseThrow(() -> new BusinessException("无权访问该对账单"));
        if (statement.getStatus() != ReconciliationStatement.Status.SUPPLIER_CONFIRMED) throw new BusinessException("对账单尚未确认");
        if (invoices.existsByStatementIdAndInvoiceNoAndDeletedFalse(statementId, request.invoiceNo())) throw new BusinessException("该对账单的发票号码已存在");
        BigDecimal registered = invoices.findByStatementIdAndDeletedFalse(statementId).stream().map(InvoiceRecord::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (registered.add(request.amount()).compareTo(statement.getSettlementAmount()) > 0) throw new BusinessException("发票累计金额超过对账金额");
        InvoiceRecord invoice = invoices.save(new InvoiceRecord(statement, request.invoiceNo(), request.amount(), request.invoiceDate(), request.attachmentKey()));
        audit.record(current.organizationCode(), current.get().getUsername(), "INVOICE", "REGISTER", "INVOICE", invoice.getId(), null, Map.of("invoiceNo", invoice.getInvoiceNo(), "amount", invoice.getAmount()));
        return InvoiceView.from(invoice);
    }
    public List<PerformanceView> performance() { return (current.isSupplier() ? performance.findBySupplierIdAndDeletedFalse(current.requireSupplier().getId()) : performance.findByOrganizationCodeAndDeletedFalse(current.organizationCode())).stream().map(PerformanceView::from).toList(); }
    @Transactional public PerformanceView score(PerformanceRequest request) {
        Supplier supplier = suppliers.findByIdAndOrganizationCodeAndDeletedFalse(request.supplierId(), current.organizationCode()).orElseThrow(() -> new BusinessException("供应商不存在"));
        SupplierPerformance result = performance.save(new SupplierPerformance(current.organizationCode(), supplier, request.periodFrom(), request.periodTo(), request.qualityScore(), request.deliveryScore(), request.costScore(), request.serviceScore()));
        if (result.isCorrectiveActionRequired()) {
            risks.save(new RiskAlert(current.organizationCode(), supplier, "PERFORMANCE_BELOW_70",
                "PERFORMANCE", String.valueOf(result.getId()), result.getGrade().equals("D") ? "HIGH" : "MEDIUM",
                "供应商周期绩效低于 70 分，需要提交整改计划", current.get().getUsername(), LocalDateTime.now().plusDays(14)));
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "PERFORMANCE", "SCORE", "SUPPLIER_PERFORMANCE", result.getId(), null, Map.of("score", result.getTotalScore(), "grade", result.getGrade()));
        return PerformanceView.from(result);
    }
    public List<RiskView> risks() { return (current.isSupplier() ? risks.findBySupplierIdAndDeletedFalse(current.requireSupplier().getId()) : risks.findByOrganizationCodeAndDeletedFalse(current.organizationCode())).stream().map(RiskView::from).toList(); }
    @Transactional public RiskView updateRisk(Long id, RiskActionRequest request) {
        RiskAlert value = risks.findById(id).filter(alert -> !alert.isDeleted() && alert.getOrganizationCode().equals(current.organizationCode())).orElseThrow(() -> new BusinessException("风险预警不存在"));
        if (current.isSupplier() && !value.getSupplier().getId().equals(current.requireSupplier().getId())) throw new BusinessException("无权访问其他供应商风险");
        if (current.isSupplier() && !"ASSIGN".equals(request.action())) throw new BusinessException("供应商只能提交整改措施，验证和关闭由采购方执行");
        switch (request.action()) {
            case "ASSIGN" -> {
                if (request.correctiveAction() == null || request.correctiveAction().isBlank() || request.dueAt() == null) throw new BusinessException("提交风险整改时必须填写措施和期限");
                String owner = request.ownerUsername() == null || request.ownerUsername().isBlank() ? current.get().getUsername() : request.ownerUsername();
                value.assignAction(request.correctiveAction(), owner, request.dueAt());
            }
            case "VERIFY" -> value.verify(); case "CLOSE" -> value.close(); default -> throw new BusinessException("不支持的风险动作");
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "RISK", request.action(), "RISK_ALERT", id, null, request);
        return RiskView.from(value);
    }
    private ReconciliationStatement scopedStatement(Long id) {
        if (current.isSupplier()) return statements.findByIdAndSupplierIdAndDeletedFalse(id, current.requireSupplier().getId()).orElseThrow(() -> new BusinessException("无权访问该对账单"));
        return statements.findById(id).filter(value -> !value.isDeleted() && value.getOrganizationCode().equals(current.organizationCode())).orElseThrow(() -> new BusinessException("对账单不存在"));
    }
    private StatementView statementView(ReconciliationStatement value) { return new StatementView(value.getId(), value.getStatementNo(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getPeriodFrom(), value.getPeriodTo(), value.getOrderAmount(), value.getReceivedAmount(), value.getSettlementAmount(), value.getStatus(), value.getDisputeReason(), statementLines.findByStatementIdAndDeletedFalseOrderByOrderOrderNoAscLineNoAsc(value.getId()).stream().map(ReconciliationLineView::from).toList(), invoices.findByStatementIdAndDeletedFalse(value.getId()).stream().map(InvoiceView::from).toList()); }
    public record GenerateStatementRequest(@NotBlank String statementNo, @NotNull Long supplierId, @NotNull LocalDate periodFrom, @NotNull LocalDate periodTo) {}
    public record StatementActionRequest(boolean confirm, String reason) {}
    public record InvoiceRequest(@NotBlank String invoiceNo, @NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull LocalDate invoiceDate, String attachmentKey) {}
    public record PerformanceRequest(@NotNull Long supplierId, @NotNull LocalDate periodFrom, @NotNull LocalDate periodTo,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal qualityScore, @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal deliveryScore,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal costScore, @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal serviceScore) {}
    public record RiskActionRequest(@NotBlank String action, String correctiveAction, String ownerUsername, LocalDateTime dueAt) {}
    public record InvoiceView(Long id, String invoiceNo, BigDecimal amount, LocalDate invoiceDate, String attachmentKey) { static InvoiceView from(InvoiceRecord value) { return new InvoiceView(value.getId(), value.getInvoiceNo(), value.getAmount(), value.getInvoiceDate(), value.getAttachmentKey()); } }
    public record ReconciliationLineView(Long id, Long orderId, String orderNo, Long orderLineId, int lineNo,
        String materialCode, BigDecimal orderedQuantity, BigDecimal receivedQuantity, BigDecimal unitPrice,
        BigDecimal orderAmount, BigDecimal settlementAmount) {
        static ReconciliationLineView from(ReconciliationLine value) { return new ReconciliationLineView(value.getId(), value.getOrder().getId(), value.getOrder().getOrderNo(), value.getOrderLine().getId(), value.getLineNo(), value.getMaterialCode(), value.getOrderedQuantity(), value.getReceivedQuantity(), value.getUnitPrice(), value.getOrderAmount(), value.getSettlementAmount()); }
    }
    public record StatementView(Long id, String statementNo, Long supplierId, String supplierName, LocalDate periodFrom,
        LocalDate periodTo, BigDecimal orderAmount, BigDecimal receivedAmount, BigDecimal settlementAmount,
        ReconciliationStatement.Status status, String disputeReason, List<ReconciliationLineView> lines,
        List<InvoiceView> invoices) {}
    public record PerformanceView(Long id, Long supplierId, String supplierName, LocalDate periodFrom, LocalDate periodTo,
        BigDecimal qualityScore, BigDecimal deliveryScore, BigDecimal costScore, BigDecimal serviceScore,
        BigDecimal totalScore, String grade, boolean correctiveActionRequired) {
        static PerformanceView from(SupplierPerformance value) { return new PerformanceView(value.getId(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getPeriodFrom(), value.getPeriodTo(), value.getQualityScore(), value.getDeliveryScore(), value.getCostScore(), value.getServiceScore(), value.getTotalScore(), value.getGrade(), value.isCorrectiveActionRequired()); }
    }
    public record RiskView(Long id, Long supplierId, String supplierName, String ruleCode, String riskLevel,
        String description, String ownerUsername, String correctiveAction, LocalDateTime dueAt, RiskAlert.Status status,
        String sourceType, String sourceId) {
        static RiskView from(RiskAlert value) { return new RiskView(value.getId(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getRuleCode(), value.getRiskLevel(), value.getDescription(), value.getOwnerUsername(), value.getCorrectiveAction(), value.getDueAt(), value.getStatus(), value.getSourceType(), value.getSourceId()); }
    }
}
