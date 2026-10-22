/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.common.ImportValidationException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/** 采购订单开放接口、Excel 全量校验导入、行级确认、版本和异常跟踪。 */
@Service
@Transactional(readOnly = true)
public class PurchaseOrderService {
    private static final List<String> TEMPLATE_HEADERS = List.of("订单号", "供应商编码", "采购组织", "工厂", "行号", "物料编码", "物料名称", "规格", "数量", "未税单价", "单位", "要求交期", "币种");
    private final PurchaseOrderRepository orders;
    private final PurchaseOrderLineRepository lines;
    private final PurchaseOrderVersionRepository versions;
    private final SupplierRepository suppliers;
    private final UserRepository users;
    private final InboxItemRepository inbox;
    private final CurrentUserService current;
    private final QuotaService quota;
    private final AuditService audit;
    private final ObjectMapper objectMapper;

    public PurchaseOrderService(PurchaseOrderRepository orders, PurchaseOrderLineRepository lines,
                                PurchaseOrderVersionRepository versions, SupplierRepository suppliers,
                                UserRepository users, InboxItemRepository inbox, CurrentUserService current,
                                QuotaService quota, AuditService audit, ObjectMapper objectMapper) {
        this.orders = orders; this.lines = lines; this.versions = versions; this.suppliers = suppliers;
        this.users = users; this.inbox = inbox; this.current = current; this.quota = quota;
        this.audit = audit; this.objectMapper = objectMapper;
    }

    public Page<OrderView> adminList(int page, int size) {
        return orders.findByOrganizationCodeAndDeletedFalse(current.organizationCode(), PageRequest.of(page, Math.min(size, 100))).map(this::view);
    }
    public List<OrderView> portalList() {
        return orders.findBySupplierIdAndDeletedFalseOrderByDeliveryDate(current.requireSupplier().getId()).stream().map(this::view).toList();
    }
    public OrderDetail detail(Long id) {
        PurchaseOrder order = scopedOrder(id);
        return new OrderDetail(view(order), lines.findByOrderIdAndDeletedFalseOrderByLineNo(id).stream().map(LineView::from).toList(),
            versions.findByOrderIdAndDeletedFalseOrderByOrderVersionDesc(id).stream().map(VersionView::from).toList());
    }

    @Transactional
    public OrderDetail create(OpenApiOrderRequest request) {
        return createValidated(request);
    }

    /** 生成与导入校验器完全一致的空白模板，避免人工制作表头导致整批失败。 */
    public byte[] importTemplate() {
        try (Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("采购订单导入");
            Row header = sheet.createRow(0);
            for (int index = 0; index < TEMPLATE_HEADERS.size(); index++) {
                header.createCell(index).setCellValue(TEMPLATE_HEADERS.get(index));
                sheet.setColumnWidth(index, Math.min(32, Math.max(12, TEMPLATE_HEADERS.get(index).length() * 3)) * 256);
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("订单导入模板生成失败", exception);
        }
    }

    @Transactional
    public ImportResult importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException("请选择订单 Excel 文件");
        List<ImportValidationException.RowError> errors = new ArrayList<>();
        List<ImportRow> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            validateHeaders(sheet.getRow(0), errors);
            DataFormatter formatter = new DataFormatter();
            for (int index = 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlank(row, formatter)) continue;
                parseRow(row, index + 1, formatter, rows, errors);
            }
        } catch (IOException | RuntimeException exception) {
            if (exception instanceof ImportValidationException validation) throw validation;
            throw new BusinessException("无法读取 Excel，请确认文件未损坏且为 xlsx 格式");
        }
        validateRows(rows, errors);
        if (!errors.isEmpty()) throw new ImportValidationException(errors);
        Map<String, List<ImportRow>> grouped = new LinkedHashMap<>();
        rows.forEach(row -> grouped.computeIfAbsent(row.orderNo(), ignored -> new ArrayList<>()).add(row));
        List<Long> ids = new ArrayList<>();
        for (List<ImportRow> group : grouped.values()) {
            ImportRow head = group.getFirst();
            OpenApiOrderRequest request = new OpenApiOrderRequest(head.orderNo(), head.supplierCode(), head.procurementOrganization(),
                head.factoryCode(), head.currency(), group.stream().map(row -> new OrderLineRequest(row.lineNo(), row.materialCode(),
                    row.materialName(), row.specification(), row.quantity(), row.unitPrice(), row.unit(), row.requestedDate())).toList());
            ids.add(createValidated(request).order().id());
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "ORDER", "IMPORT", "PURCHASE_ORDER", ids,
            null, Map.of("fileName", file.getOriginalFilename() == null ? "" : file.getOriginalFilename(), "orderCount", ids.size(), "lineCount", rows.size()));
        return new ImportResult(ids.size(), rows.size(), ids);
    }

    @Transactional
    public OrderDetail confirmLine(Long orderId, Long lineId, ConfirmLineRequest request) {
        PurchaseOrder order = supplierOrder(orderId);
        ensureConfirmable(order);
        PurchaseOrderLine line = lines.findById(lineId).orElseThrow(() -> new BusinessException("订单行不存在"));
        if (!line.getOrder().getId().equals(orderId)) throw new BusinessException("订单行不属于该订单");
        try { line.confirm(request.confirmedQuantity(), request.confirmedDate(), request.remark()); }
        catch (IllegalArgumentException exception) { throw new BusinessException(exception.getMessage()); }
        List<PurchaseOrderLine> all = lines.findByOrderIdAndDeletedFalseOrderByLineNo(orderId);
        boolean allResponded = all.stream().allMatch(value -> value.getConfirmStatus() != PurchaseOrderLine.ConfirmStatus.PENDING);
        if (allResponded && order.getStatus() != PurchaseOrder.Status.CONFIRMED) order.acknowledge();
        else if (!allResponded) order.markPartiallyConfirmed();
        QuotaService.ConsumptionResult consumption = quota.synchronizeOrderUsage(order.getOrganizationCode(),
            order.getProcurementOrganization(), order.getFactoryCode(), line.getMaterialCode(), line.getRequestedDate(),
            order.getSupplier().getId(), request.confirmedQuantity(), quotaSource(order, line), current.get().getUsername());
        if (request.confirmedQuantity().compareTo(line.getOrderedQuantity()) < 0) {
            String message = "供应商确认数量不足：订单行 " + line.getLineNo();
            order.markAbnormal(message); createBuyerAlert(order, message);
        }
        if (request.confirmedDate().isAfter(line.getRequestedDate())) {
            String message = "供应商反馈交期晚于要求：订单行 " + line.getLineNo();
            order.markAbnormal(message); createBuyerAlert(order, message);
        }
        if (consumption.warning()) { order.markAbnormal(consumption.message()); createBuyerAlert(order, consumption.message()); }
        audit.record(order.getOrganizationCode(), current.get().getUsername(), "ORDER", "LINE_CONFIRM", "PURCHASE_ORDER_LINE",
            lineId, null, Map.of("quantity", request.confirmedQuantity(), "date", request.confirmedDate(), "quota", consumption.message()));
        return detail(orderId);
    }

    @Transactional
    public OrderDetail confirmOrder(Long orderId, ConfirmOrderRequest request) {
        PurchaseOrder order = supplierOrder(orderId);
        ensureConfirmable(order);
        List<PurchaseOrderLine> values = lines.findByOrderIdAndDeletedFalseOrderByLineNo(orderId);
        if (values.isEmpty()) throw new BusinessException("订单没有可确认明细");
        for (PurchaseOrderLine line : values) {
            LocalDate date = request.confirmedDate() == null ? line.getRequestedDate() : request.confirmedDate();
            try { line.confirm(line.getOrderedQuantity(), date, request.remark()); }
            catch (IllegalArgumentException exception) { throw new BusinessException(exception.getMessage()); }
            QuotaService.ConsumptionResult result = quota.synchronizeOrderUsage(order.getOrganizationCode(),
                order.getProcurementOrganization(), order.getFactoryCode(), line.getMaterialCode(), line.getRequestedDate(),
                order.getSupplier().getId(), line.getOrderedQuantity(), quotaSource(order, line), current.get().getUsername());
            if (result.warning()) { order.markAbnormal(result.message()); createBuyerAlert(order, result.message()); }
            if (date.isAfter(line.getRequestedDate())) {
                String message = "供应商反馈整单交期晚于要求";
                order.markAbnormal(message); createBuyerAlert(order, message);
            }
        }
        order.acknowledge();
        audit.record(order.getOrganizationCode(), current.get().getUsername(), "ORDER", "ORDER_CONFIRM",
            "PURCHASE_ORDER", orderId, null, Map.of("confirmedDate", request.confirmedDate() == null ? "按订单行要求" : request.confirmedDate(), "remark", request.remark() == null ? "" : request.remark()));
        return detail(orderId);
    }

    @Transactional
    public OrderDetail change(Long orderId, ChangeOrderRequest request) {
        PurchaseOrder order = adminOrder(orderId);
        if (order.getStatus() == PurchaseOrder.Status.COMPLETED || order.getStatus() == PurchaseOrder.Status.CANCELLED) {
            throw new BusinessException("已完成或已取消订单不能创建变更版本");
        }
        int previousVersion = order.getOrderVersion();
        versions.save(new PurchaseOrderVersion(order, previousVersion, snapshot(orderId), request.reason(), current.get().getUsername()));
        Map<Long, PurchaseOrderLine> existing = lines.findByOrderIdAndDeletedFalseOrderByLineNo(orderId).stream()
            .collect(java.util.stream.Collectors.toMap(PurchaseOrderLine::getId, value -> value));
        for (ChangeLineRequest change : request.lines() == null ? List.<ChangeLineRequest>of() : request.lines()) {
            PurchaseOrderLine line = existing.get(change.lineId());
            if (line == null) throw new BusinessException("变更订单行不存在：" + change.lineId());
            try { line.revise(change.quantity(), change.unitPrice(), change.requestedDate()); }
            catch (IllegalArgumentException exception) { throw new BusinessException(exception.getMessage()); }
        }
        BigDecimal amount = existing.values().stream().map(PurchaseOrderLine::lineAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate latest = existing.values().stream().map(PurchaseOrderLine::getRequestedDate).max(LocalDate::compareTo)
            .orElse(request.deliveryDate());
        order.recalculate(amount, request.deliveryDate() == null ? latest : request.deliveryDate());
        order.createVersion(request.reason());
        for (UserAccount user : users.findBySupplierIdAndEnabledTrue(order.getSupplier().getId())) {
            inbox.save(new InboxItem(order.getOrganizationCode(), user.getUsername(), order.getSupplier(), InboxItem.Kind.TASK,
                "采购订单已变更", order.getOrderNo() + " 已生成版本 " + order.getOrderVersion(), "ORDER", String.valueOf(orderId),
                "/portal/orders/" + orderId, java.time.LocalDateTime.now().plusDays(2)));
        }
        audit.record(order.getOrganizationCode(), current.get().getUsername(), "ORDER", "CHANGE", "PURCHASE_ORDER", orderId,
            Map.of("version", previousVersion), Map.of("version", order.getOrderVersion(), "deliveryDate", order.getDeliveryDate(), "amount", amount, "reason", request.reason()));
        return detail(orderId);
    }

    private OrderDetail createValidated(OpenApiOrderRequest request) {
        String org = current.organizationCode();
        if (orders.findByOrderNoAndOrganizationCodeAndDeletedFalse(request.orderNo(), org).isPresent()) throw new BusinessException("订单号已存在，未重复创建");
        Supplier supplier = suppliers.findBySupplierCodeAndOrganizationCodeAndDeletedFalse(request.supplierCode(), org)
            .orElseThrow(() -> new BusinessException("供应商编码不存在"));
        BigDecimal amount = request.lines().stream().map(line -> line.quantity().multiply(line.unitPrice())).reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate deliveryDate = request.lines().stream().map(OrderLineRequest::requestedDate).max(LocalDate::compareTo).orElseThrow();
        String summary = request.lines().size() == 1 ? request.lines().getFirst().materialName() : request.lines().getFirst().materialName() + " 等 " + request.lines().size() + " 项";
        PurchaseOrder order = orders.save(new PurchaseOrder(org, request.orderNo(), supplier, request.procurementOrganization(),
            request.factoryCode(), summary, amount, request.currency(), deliveryDate));
        Set<Integer> lineNumbers = new HashSet<>();
        for (OrderLineRequest value : request.lines()) {
            if (!lineNumbers.add(value.lineNo())) throw new BusinessException("订单行号重复：" + value.lineNo());
            lines.save(new PurchaseOrderLine(order, value.lineNo(), value.materialCode(), value.materialName(), value.specification(),
                value.quantity(), value.unitPrice(), value.unit(), value.requestedDate()));
        }
        for (UserAccount user : users.findBySupplierIdAndEnabledTrue(supplier.getId())) {
            inbox.save(new InboxItem(org, user.getUsername(), supplier, InboxItem.Kind.TASK, "采购订单待确认",
                order.getOrderNo() + " 共 " + request.lines().size() + " 行", "ORDER", String.valueOf(order.getId()),
                "/portal/orders/" + order.getId(), java.time.LocalDateTime.now().plusDays(2)));
        }
        audit.record(org, current.get().getUsername(), "ORDER", "CREATE", "PURCHASE_ORDER", order.getId(), null,
            Map.of("orderNo", order.getOrderNo(), "supplier", supplier.getSupplierCode(), "amount", amount));
        return detail(order.getId());
    }

    private void validateHeaders(Row header, List<ImportValidationException.RowError> errors) {
        DataFormatter formatter = new DataFormatter();
        if (header == null) { errors.add(new ImportValidationException.RowError(1, "表头", "缺少表头")); return; }
        for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) {
            String actual = formatter.formatCellValue(header.getCell(i)).trim();
            if (!TEMPLATE_HEADERS.get(i).equals(actual)) errors.add(new ImportValidationException.RowError(1, "第 " + (i + 1) + " 列", "应为“" + TEMPLATE_HEADERS.get(i) + "”，实际为“" + actual + "”"));
        }
    }
    private void parseRow(Row row, int rowNumber, DataFormatter f, List<ImportRow> target, List<ImportValidationException.RowError> errors) {
        try {
            target.add(new ImportRow(rowNumber, text(row, 0, f), text(row, 1, f), text(row, 2, f), text(row, 3, f),
                integer(row, 4, f), text(row, 5, f), text(row, 6, f), f.formatCellValue(row.getCell(7)).trim(),
                decimal(row, 8, f), decimal(row, 9, f), text(row, 10, f), date(row, 11, f), text(row, 12, f)));
        } catch (IllegalArgumentException exception) { errors.add(new ImportValidationException.RowError(rowNumber, "行数据", exception.getMessage())); }
    }
    private void validateRows(List<ImportRow> rows, List<ImportValidationException.RowError> errors) {
        if (rows.isEmpty()) errors.add(new ImportValidationException.RowError(2, "数据", "没有可导入的订单行"));
        Set<String> keys = new HashSet<>();
        for (ImportRow row : rows) {
            if (!keys.add(row.orderNo() + "#" + row.lineNo())) errors.add(new ImportValidationException.RowError(row.rowNumber(), "行号", "同一订单行号重复"));
            if (row.quantity().signum() <= 0) errors.add(new ImportValidationException.RowError(row.rowNumber(), "数量", "必须大于零"));
            if (row.unitPrice().signum() < 0) errors.add(new ImportValidationException.RowError(row.rowNumber(), "未税单价", "不能小于零"));
            if (orders.findByOrderNoAndOrganizationCodeAndDeletedFalse(row.orderNo(), current.organizationCode()).isPresent()) errors.add(new ImportValidationException.RowError(row.rowNumber(), "订单号", "系统中已存在"));
            if (suppliers.findBySupplierCodeAndOrganizationCodeAndDeletedFalse(row.supplierCode(), current.organizationCode()).isEmpty()) errors.add(new ImportValidationException.RowError(row.rowNumber(), "供应商编码", "系统中不存在"));
        }
        Map<String, ImportRow> heads = new HashMap<>();
        for (ImportRow row : rows) {
            ImportRow head = heads.putIfAbsent(row.orderNo(), row);
            if (head != null && (!head.supplierCode().equals(row.supplierCode()) || !head.factoryCode().equals(row.factoryCode()) || !head.procurementOrganization().equals(row.procurementOrganization()) || !head.currency().equals(row.currency()))) {
                errors.add(new ImportValidationException.RowError(row.rowNumber(), "订单头", "同一订单号的供应商、采购组织、工厂和币种必须一致"));
            }
        }
    }
    private String text(Row row, int index, DataFormatter formatter) { String value = formatter.formatCellValue(row.getCell(index)).trim(); if (value.isBlank()) throw new IllegalArgumentException(TEMPLATE_HEADERS.get(index) + "不能为空"); return value; }
    private int integer(Row row, int index, DataFormatter formatter) { try { return new BigDecimal(text(row, index, formatter)).intValueExact(); } catch (ArithmeticException | NumberFormatException exception) { throw new IllegalArgumentException(TEMPLATE_HEADERS.get(index) + "必须为整数"); } }
    private BigDecimal decimal(Row row, int index, DataFormatter formatter) { try { return new BigDecimal(text(row, index, formatter).replace(",", "")); } catch (NumberFormatException exception) { throw new IllegalArgumentException(TEMPLATE_HEADERS.get(index) + "必须为数字"); } }
    private LocalDate date(Row row, int index, DataFormatter formatter) { Cell cell = row.getCell(index); try { if (cell != null && DateUtil.isCellDateFormatted(cell)) return cell.getLocalDateTimeCellValue().toLocalDate(); return LocalDate.parse(text(row, index, formatter)); } catch (DateTimeParseException | IllegalStateException exception) { throw new IllegalArgumentException(TEMPLATE_HEADERS.get(index) + "应为 yyyy-MM-dd"); } }
    private boolean isBlank(Row row, DataFormatter formatter) { for (int i = 0; i < TEMPLATE_HEADERS.size(); i++) if (!formatter.formatCellValue(row.getCell(i)).isBlank()) return false; return true; }

    private void createBuyerAlert(PurchaseOrder order, String message) {
        for (UserAccount user : users.findByOrganizationCodeAndRoleAndEnabledTrue(order.getOrganizationCode(), UserAccount.Role.BUYER)) {
            inbox.save(new InboxItem(order.getOrganizationCode(), user.getUsername(), null, InboxItem.Kind.ALERT,
                "订单配额异常", order.getOrderNo() + "：" + message, "ORDER", String.valueOf(order.getId()),
                "/admin/orders/" + order.getId(), java.time.LocalDateTime.now().plusDays(1)));
        }
    }
    private PurchaseOrder adminOrder(Long id) { return orders.findById(id).filter(value -> !value.isDeleted() && value.getOrganizationCode().equals(current.organizationCode())).orElseThrow(() -> new BusinessException("采购订单不存在")); }
    private PurchaseOrder supplierOrder(Long id) { return orders.findByIdAndSupplierIdAndDeletedFalse(id, current.requireSupplier().getId()).orElseThrow(() -> new BusinessException("无权访问该订单")); }
    private PurchaseOrder scopedOrder(Long id) { return current.isSupplier() ? supplierOrder(id) : adminOrder(id); }
    private void ensureConfirmable(PurchaseOrder order) {
        if (order.getStatus() != PurchaseOrder.Status.PENDING_ACK
            && order.getStatus() != PurchaseOrder.Status.PENDING_CONFIRMATION
            && order.getStatus() != PurchaseOrder.Status.PARTIALLY_CONFIRMED) {
            throw new BusinessException("订单当前状态不可确认");
        }
    }
    private String snapshot(Long orderId) { try { return objectMapper.writeValueAsString(detail(orderId)); } catch (JsonProcessingException exception) { throw new IllegalStateException("订单版本快照生成失败：" + exception.getOriginalMessage(), exception); } }
    private String quotaSource(PurchaseOrder order, PurchaseOrderLine line) { return order.getOrderNo() + "-" + line.getLineNo(); }
    private OrderView view(PurchaseOrder value) { return new OrderView(value.getId(), value.getOrderNo(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getProcurementOrganization(), value.getFactoryCode(), value.getItemSummary(), value.getAmount(), value.getCurrency(), value.getDeliveryDate(), value.getDeliveredPercent(), value.getOrderVersion(), value.isAbnormal(), value.getAbnormalReason(), value.getStatus()); }

    public record OrderLineRequest(@Min(1) int lineNo, @NotBlank String materialCode, @NotBlank String materialName,
        String specification, @NotNull @DecimalMin("0.0001") BigDecimal quantity, @NotNull @DecimalMin("0") BigDecimal unitPrice,
        @NotBlank String unit, @NotNull LocalDate requestedDate) {}
    public record OpenApiOrderRequest(@NotBlank String orderNo, @NotBlank String supplierCode,
        @NotBlank String procurementOrganization, @NotBlank String factoryCode, @NotBlank String currency,
        @NotEmpty List<@Valid OrderLineRequest> lines) {}
    public record ConfirmLineRequest(@NotNull @DecimalMin("0.0001") BigDecimal confirmedQuantity,
        @NotNull LocalDate confirmedDate, @Size(max = 500) String remark) {}
    public record ConfirmOrderRequest(LocalDate confirmedDate, @Size(max = 500) String remark) {}
    public record ChangeLineRequest(@NotNull Long lineId, @NotNull @DecimalMin("0.0001") BigDecimal quantity,
        @NotNull @DecimalMin("0") BigDecimal unitPrice, @NotNull LocalDate requestedDate) {}
    public record ChangeOrderRequest(LocalDate deliveryDate, @NotBlank @Size(max = 500) String reason,
        List<@Valid ChangeLineRequest> lines) {}
    public record OrderView(Long id, String orderNo, Long supplierId, String supplierName, String procurementOrganization,
        String factoryCode, String itemSummary, BigDecimal amount, String currency, LocalDate deliveryDate,
        int deliveredPercent, int version, boolean abnormal, String abnormalReason, PurchaseOrder.Status status) {}
    public record LineView(Long id, int lineNo, String materialCode, String materialName, String specification,
        BigDecimal orderedQuantity, BigDecimal unitPrice, String unit, LocalDate requestedDate,
        BigDecimal confirmedQuantity, LocalDate confirmedDate, BigDecimal receivedQuantity,
        String supplierRemark, PurchaseOrderLine.ConfirmStatus confirmStatus) {
        static LineView from(PurchaseOrderLine value) { return new LineView(value.getId(), value.getLineNo(), value.getMaterialCode(), value.getMaterialName(), value.getSpecification(), value.getOrderedQuantity(), value.getUnitPrice(), value.getUnit(), value.getRequestedDate(), value.getConfirmedQuantity(), value.getConfirmedDate(), value.getReceivedQuantity(), value.getSupplierRemark(), value.getConfirmStatus()); }
    }
    public record VersionView(Long id, int version, String reason, String changedBy, java.time.LocalDateTime changedAt) {
        static VersionView from(PurchaseOrderVersion value) { return new VersionView(value.getId(), value.getOrderVersion(), value.getChangeReason(), value.getChangedBy(), value.getCreatedAt()); }
    }
    public record OrderDetail(OrderView order, List<LineView> lines, List<VersionView> versions) {}
    public record ImportResult(int ordersCreated, int linesCreated, List<Long> orderIds) {}
    private record ImportRow(int rowNumber, String orderNo, String supplierCode, String procurementOrganization,
        String factoryCode, int lineNo, String materialCode, String materialName, String specification,
        BigDecimal quantity, BigDecimal unitPrice, String unit, LocalDate requestedDate, String currency) {}
}
