/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/** ASN、收货差异、NCR/CAR 和 8D 整改闭环。 */
@Service @Transactional(readOnly = true)
public class DeliveryQualityService {
    private final AsnRepository asns; private final AsnLineRepository asnLines;
    private final PurchaseOrderRepository orders; private final PurchaseOrderLineRepository orderLines;
    private final QualityIssueRepository quality; private final SupplierRepository suppliers;
    private final CurrentUserService current; private final AuditService audit;
    public DeliveryQualityService(AsnRepository asns, AsnLineRepository asnLines, PurchaseOrderRepository orders,
                                  PurchaseOrderLineRepository orderLines, QualityIssueRepository quality,
                                  SupplierRepository suppliers, CurrentUserService current, AuditService audit) {
        this.asns = asns; this.asnLines = asnLines; this.orders = orders; this.orderLines = orderLines;
        this.quality = quality; this.suppliers = suppliers; this.current = current; this.audit = audit;
    }
    public List<AsnView> asnList() { return (current.isSupplier() ? asns.findBySupplierIdAndDeletedFalse(current.requireSupplier().getId()) : asns.findByOrganizationCodeAndDeletedFalse(current.organizationCode())).stream().map(AsnView::from).toList(); }
    @Transactional public AsnDetail createAsn(CreateAsnRequest request) {
        Supplier supplier = current.requireSupplier();
        PurchaseOrder order = orders.findByIdAndSupplierIdAndDeletedFalse(request.orderId(), supplier.getId()).orElseThrow(() -> new BusinessException("无权为该订单创建 ASN"));
        if (asns.findBySupplierIdAndDeletedFalse(supplier.getId()).stream().anyMatch(value -> value.getAsnNo().equals(request.asnNo()))) throw new BusinessException("ASN 编号已存在");
        AdvanceShippingNotice asn = asns.save(new AdvanceShippingNotice(order.getOrganizationCode(), request.asnNo(), supplier, order, request.plannedArrivalAt()));
        for (AsnLineRequest lineRequest : request.lines()) {
            PurchaseOrderLine line = orderLines.findById(lineRequest.orderLineId()).orElseThrow(() -> new BusinessException("订单行不存在"));
            if (!line.getOrder().getId().equals(order.getId())) throw new BusinessException("ASN 行不属于该订单");
            if (lineRequest.shippedQuantity().compareTo(line.getOrderedQuantity().subtract(line.getReceivedQuantity())) > 0) throw new BusinessException("发货数量超过订单未收数量");
            asnLines.save(new AsnLine(asn, line, lineRequest.shippedQuantity()));
        }
        audit.record(order.getOrganizationCode(), current.get().getUsername(), "DELIVERY", "ASN_CREATE", "ASN", asn.getId(), null, Map.of("asnNo", asn.getAsnNo()));
        return asnDetail(asn.getId());
    }
    @Transactional public AsnDetail appoint(Long id, AppointmentRequest request) { AdvanceShippingNotice asn = scopedAsn(id); asn.appoint(request.appointmentAt()); return asnDetail(id); }
    @Transactional public AsnDetail ship(Long id, ShipRequest request) { AdvanceShippingNotice asn = supplierAsn(id); asn.ship(request.carrierName(), request.trackingNo()); asn.getOrder().updateStatus(PurchaseOrder.Status.SHIPPING); audit.record(asn.getOrganizationCode(), current.get().getUsername(), "DELIVERY", "SHIP", "ASN", id, null, request); return asnDetail(id); }
    @Transactional public AsnDetail receive(Long id, ReceiveRequest request) {
        AdvanceShippingNotice asn = adminAsn(id);
        List<AsnLine> values = asnLines.findByAsnIdAndDeletedFalse(id);
        for (ReceiveLineRequest value : request.lines()) {
            AsnLine line = values.stream().filter(item -> item.getId().equals(value.asnLineId())).findFirst().orElseThrow(() -> new BusinessException("ASN 行不存在"));
            BigDecimal delta = value.receivedQuantity().subtract(line.getReceivedQuantity());
            if (delta.signum() < 0) throw new BusinessException("实收数量不能小于已登记数量");
            line.receive(value.receivedQuantity(), value.discrepancyReason());
            if (delta.signum() > 0) line.getOrderLine().receive(delta);
        }
        boolean complete = values.stream().allMatch(line -> line.getReceivedQuantity().compareTo(line.getShippedQuantity()) == 0);
        asn.receive(complete, request.discrepancyReason());
        updateOrderReceipt(asn.getOrder());
        audit.record(asn.getOrganizationCode(), current.get().getUsername(), "DELIVERY", "RECEIVE", "ASN", id, null, Map.of("complete", complete));
        return asnDetail(id);
    }
    public AsnDetail asnDetail(Long id) { AdvanceShippingNotice asn = scopedAsn(id); return new AsnDetail(AsnView.from(asn), asnLines.findByAsnIdAndDeletedFalse(id).stream().map(AsnLineView::from).toList()); }

    public List<QualityView> qualityList() { return (current.isSupplier() ? quality.findBySupplierIdAndDeletedFalse(current.requireSupplier().getId()) : quality.findByOrganizationCodeAndDeletedFalse(current.organizationCode())).stream().map(QualityView::from).toList(); }
    public QualityDetail qualityDetail(Long id) {
        QualityIssue issue = current.isSupplier() ? supplierQuality(id) : adminQuality(id);
        return new QualityDetail(QualityView.from(issue), audit.history(issue.getOrganizationCode(), "QUALITY_ISSUE", id).stream().map(AuditHistoryView::from).toList());
    }
    @Transactional public QualityView createQuality(CreateQualityRequest request) {
        if (quality.existsByOrganizationCodeAndIssueNoAndDeletedFalse(current.organizationCode(), request.issueNo())) throw new BusinessException("质量问题编号已存在");
        Supplier supplier = suppliers.findByIdAndOrganizationCodeAndDeletedFalse(request.supplierId(), current.organizationCode()).orElseThrow(() -> new BusinessException("供应商不存在"));
        QualityIssue issue = quality.save(new QualityIssue(current.organizationCode(), request.issueNo(), request.type(), supplier, request.title(), request.severity(), request.dueDate(), current.get().getFullName()));
        audit.record(current.organizationCode(), current.get().getUsername(), "QUALITY", "CREATE", "QUALITY_ISSUE", issue.getId(), null, Map.of("issueNo", issue.getIssueNo()));
        return QualityView.from(issue);
    }
    @Transactional public QualityView startAnalysis(Long id) { QualityIssue issue = supplierQuality(id); issue.startAnalysis(); audit.record(issue.getOrganizationCode(), current.get().getUsername(), "QUALITY", "START_ANALYSIS", "QUALITY_ISSUE", id, null, Map.of("status", issue.getStatus())); return QualityView.from(issue); }
    @Transactional public QualityView submitAction(Long id, CorrectiveActionRequest request) {
        QualityIssue issue = supplierQuality(id); issue.submitAction(request.rootCause(), request.interimAction(), request.permanentAction(), request.evidenceFileKey());
        audit.record(issue.getOrganizationCode(), current.get().getUsername(), "QUALITY", "ACTION_SUBMIT", "QUALITY_ISSUE", id, null, request);
        return QualityView.from(issue);
    }
    @Transactional public QualityView reviewAction(Long id, ReviewActionRequest request) {
        QualityIssue issue = adminQuality(id);
        if (request.approved()) issue.startVerification(); else issue.returnAction(request.comment());
        audit.record(issue.getOrganizationCode(), current.get().getUsername(), "QUALITY", request.approved() ? "ACTION_ACCEPT" : "ACTION_RETURN", "QUALITY_ISSUE", id, null, request);
        return QualityView.from(issue);
    }
    @Transactional public QualityView close(Long id, CloseQualityRequest request) { QualityIssue issue = adminQuality(id); issue.close(request.verificationResult()); audit.record(issue.getOrganizationCode(), current.get().getUsername(), "QUALITY", "CLOSE", "QUALITY_ISSUE", id, null, request); return QualityView.from(issue); }

    /** 导出当前采购组织的质量问题，字段与页面一致，便于线下复盘和审计。 */
    public byte[] exportQuality() {
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("质量问题");
            String[] headers = {"问题编号", "类型", "供应商", "标题", "严重程度", "责任人", "要求完成日", "状态", "根因", "临时措施", "永久措施", "验证结果"};
            var header = sheet.createRow(0);
            CellStyle headerStyle = workbook.createCellStyle();
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_CORNFLOWER_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            for (int index = 0; index < headers.length; index++) { header.createCell(index).setCellValue(headers[index]); header.getCell(index).setCellStyle(headerStyle); }
            int rowIndex = 1;
            for (QualityIssue issue : quality.findByOrganizationCodeAndDeletedFalse(current.organizationCode())) {
                var row = sheet.createRow(rowIndex++);
                String[] values = { issue.getIssueNo(), issue.getIssueType().name(), issue.getSupplier().getSupplierName(), issue.getTitle(), issue.getSeverity(), issue.getOwnerName(), value(issue.getDueDate()), issue.getStatus().name(), value(issue.getRootCause()), value(issue.getInterimAction()), value(issue.getPermanentAction()), value(issue.getVerificationResult()) };
                for (int index = 0; index < values.length; index++) row.createCell(index).setCellValue(values[index]);
            }
            for (int index = 0; index < headers.length; index++) sheet.autoSizeColumn(index);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) { throw new BusinessException("质量问题导出失败"); }
    }

    private String value(Object value) { return value == null ? "" : String.valueOf(value); }

    private void updateOrderReceipt(PurchaseOrder order) {
        List<PurchaseOrderLine> values = orderLines.findByOrderIdAndDeletedFalseOrderByLineNo(order.getId());
        BigDecimal ordered = values.stream().map(PurchaseOrderLine::getOrderedQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal received = values.stream().map(PurchaseOrderLine::getReceivedQuantity).reduce(BigDecimal.ZERO, BigDecimal::add);
        int percent = ordered.signum() == 0 ? 0 : received.multiply(new BigDecimal("100")).divide(ordered, 0, RoundingMode.DOWN).intValue();
        order.updateReceiptPercent(percent);
    }
    private AdvanceShippingNotice scopedAsn(Long id) { return current.isSupplier() ? supplierAsn(id) : adminAsn(id); }
    private AdvanceShippingNotice supplierAsn(Long id) { return asns.findByIdAndSupplierIdAndDeletedFalse(id, current.requireSupplier().getId()).orElseThrow(() -> new BusinessException("无权访问该 ASN")); }
    private AdvanceShippingNotice adminAsn(Long id) { return asns.findById(id).filter(value -> !value.isDeleted() && value.getOrganizationCode().equals(current.organizationCode())).orElseThrow(() -> new BusinessException("ASN 不存在")); }
    private QualityIssue supplierQuality(Long id) { return quality.findByIdAndSupplierIdAndDeletedFalse(id, current.requireSupplier().getId()).orElseThrow(() -> new BusinessException("无权访问该质量问题")); }
    private QualityIssue adminQuality(Long id) { return quality.findById(id).filter(value -> !value.isDeleted() && value.getOrganizationCode().equals(current.organizationCode())).orElseThrow(() -> new BusinessException("质量问题不存在")); }

    public record AsnLineRequest(@NotNull Long orderLineId, @NotNull @DecimalMin("0.0001") BigDecimal shippedQuantity) {}
    public record CreateAsnRequest(@NotBlank String asnNo, @NotNull Long orderId, @NotNull @Future LocalDateTime plannedArrivalAt,
                                   @NotEmpty List<@Valid AsnLineRequest> lines) {}
    public record AppointmentRequest(@NotNull @Future LocalDateTime appointmentAt) {}
    public record ShipRequest(@NotBlank String carrierName, @NotBlank String trackingNo) {}
    public record ReceiveLineRequest(@NotNull Long asnLineId, @NotNull @DecimalMin("0") BigDecimal receivedQuantity, String discrepancyReason) {}
    public record ReceiveRequest(@NotEmpty List<@Valid ReceiveLineRequest> lines, String discrepancyReason) {}
    public record AsnView(Long id, String asnNo, Long orderId, String orderNo, Long supplierId, String supplierName,
        LocalDateTime plannedArrivalAt, LocalDateTime appointmentAt, LocalDateTime shippedAt, LocalDateTime receivedAt,
        String carrierName, String trackingNo, String discrepancyReason, AdvanceShippingNotice.Status status) {
        static AsnView from(AdvanceShippingNotice value) { return new AsnView(value.getId(), value.getAsnNo(), value.getOrder().getId(), value.getOrder().getOrderNo(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getPlannedArrivalAt(), value.getAppointmentAt(), value.getShippedAt(), value.getReceivedAt(), value.getCarrierName(), value.getTrackingNo(), value.getDiscrepancyReason(), value.getStatus()); }
    }
    public record AsnLineView(Long id, Long orderLineId, String materialCode, BigDecimal plannedQuantity,
        BigDecimal shippedQuantity, BigDecimal receivedQuantity, BigDecimal differenceQuantity, String discrepancyReason) {
        static AsnLineView from(AsnLine value) { return new AsnLineView(value.getId(), value.getOrderLine().getId(), value.getOrderLine().getMaterialCode(), value.getOrderLine().getOrderedQuantity(), value.getShippedQuantity(), value.getReceivedQuantity(), value.differenceQuantity(), value.getDiscrepancyReason()); }
    }
    public record AsnDetail(AsnView asn, List<AsnLineView> lines) {}
    public record CreateQualityRequest(@NotBlank String issueNo, @NotNull Long supplierId, @NotNull QualityIssue.Type type,
        @NotBlank String title, @NotBlank String severity, @FutureOrPresent LocalDate dueDate) {}
    public record CorrectiveActionRequest(@NotBlank String rootCause, @NotBlank String interimAction,
        @NotBlank String permanentAction, String evidenceFileKey) {}
    public record ReviewActionRequest(boolean approved, @NotBlank String comment) {}
    public record CloseQualityRequest(@NotBlank String verificationResult) {}
    public record AuditHistoryView(Long id, String actor, String module, String action, String beforeContent,
                                   String afterContent, LocalDateTime occurredAt) {
        static AuditHistoryView from(AuditLog value) { return new AuditHistoryView(value.getId(), value.getActor(), value.getModule(), value.getAction(), value.getBeforeContent(), value.getAfterContent(), value.getOccurredAt()); }
    }
    public record QualityView(Long id, String issueNo, QualityIssue.Type type, Long supplierId, String supplierName,
        String title, String severity, LocalDate dueDate, String ownerName, String rootCause, String interimAction,
        String permanentAction, String verificationResult, String returnReason, String evidenceFileKey, QualityIssue.Status status) {
        static QualityView from(QualityIssue value) { return new QualityView(value.getId(), value.getIssueNo(), value.getIssueType(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getTitle(), value.getSeverity(), value.getDueDate(), value.getOwnerName(), value.getRootCause(), value.getInterimAction(), value.getPermanentAction(), value.getVerificationResult(), value.getReturnReason(), value.getEvidenceFileKey(), value.getStatus()); }
    }
    public record QualityDetail(QualityView issue, List<AuditHistoryView> history) {}
}
