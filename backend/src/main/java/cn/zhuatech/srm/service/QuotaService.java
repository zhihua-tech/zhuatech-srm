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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/** 配额方案、供应商分配、版本生效、执行余额和 Excel 导出。 */
@Service
@Transactional(readOnly = true)
public class QuotaService {
    private final QuotaPlanRepository plans;
    private final QuotaAllocationRepository allocations;
    private final QuotaUsageRepository usages;
    private final SupplierRepository suppliers;
    private final CurrentUserService current;
    private final AuditService audit;

    public QuotaService(QuotaPlanRepository plans, QuotaAllocationRepository allocations,
                        QuotaUsageRepository usages, SupplierRepository suppliers,
                        CurrentUserService current, AuditService audit) {
        this.plans = plans; this.allocations = allocations; this.usages = usages;
        this.suppliers = suppliers; this.current = current; this.audit = audit;
    }

    public Page<PlanView> list(int page, int size) {
        return plans.findByOrganizationCodeAndDeletedFalse(current.organizationCode(), PageRequest.of(page, Math.min(size, 100)))
            .map(this::view);
    }
    public PlanDetail detail(Long id) {
        QuotaPlan plan = scopedPlan(id);
        return new PlanDetail(view(plan), allocations.findByPlanIdAndDeletedFalse(id).stream().map(AllocationView::from).toList(), warnings(plan));
    }

    @Transactional
    public PlanDetail create(CreatePlanRequest request) {
        if (request.validTo().isBefore(request.validFrom())) throw new BusinessException("配额结束日期不能早于开始日期");
        if (request.totalQuantity().signum() <= 0) throw new BusinessException("计划总数量必须大于零");
        String org = current.organizationCode();
        int nextVersion = plans.findByOrganizationCodeAndDeletedFalse(org, Pageable.unpaged())
            .stream().filter(p -> p.getProcurementOrganization().equals(request.procurementOrganization())
                && p.getFactoryCode().equals(request.factoryCode()) && p.getMaterialCode().equals(request.materialCode())
                && p.getValidFrom().equals(request.validFrom()) && p.getValidTo().equals(request.validTo()))
            .mapToInt(QuotaPlan::getPlanVersion).max().orElse(0) + 1;
        QuotaPlan plan = plans.save(new QuotaPlan(org, request.procurementOrganization(), request.factoryCode(),
            request.materialCode(), request.materialName(), request.quotaType(), request.totalQuantity(),
            request.validFrom(), request.validTo(), nextVersion, request.changeReason()));
        Set<Long> supplierIds = new HashSet<>();
        for (AllocationRequest value : request.allocations()) {
            if (!supplierIds.add(value.supplierId())) throw new BusinessException("同一供应商不能重复分配");
            Supplier supplier = suppliers.findByIdAndOrganizationCodeAndDeletedFalse(value.supplierId(), org)
                .orElseThrow(() -> new BusinessException("供应商不存在：" + value.supplierId()));
            BigDecimal allocatedQuantity = request.quotaType() == QuotaPlan.Type.RATIO
                ? request.totalQuantity().multiply(value.value()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                : value.value();
            allocations.save(new QuotaAllocation(plan, supplier, value.value(), allocatedQuantity, value.note()));
        }
        audit.record(org, current.get().getUsername(), "QUOTA", "CREATE", "QUOTA_PLAN", plan.getId(), null,
            Map.of("materialCode", plan.getMaterialCode(), "version", plan.getPlanVersion()));
        return detail(plan.getId());
    }

    @Transactional
    public PlanDetail activate(Long id) {
        QuotaPlan plan = scopedPlan(id);
        List<QuotaAllocation> values = allocations.findByPlanIdAndDeletedFalse(id);
        validateAllocationTotal(plan, values);
        List<QuotaPlan> active = plans.findByOrganizationCodeAndProcurementOrganizationAndFactoryCodeAndMaterialCodeAndStatusAndDeletedFalse(
            plan.getOrganizationCode(), plan.getProcurementOrganization(), plan.getFactoryCode(), plan.getMaterialCode(), QuotaPlan.Status.ACTIVE);
        if (active.stream().anyMatch(other -> !other.getId().equals(id) && overlaps(plan, other))) {
            throw new BusinessException("同一组织、工厂、物料和有效期范围已存在生效配额版本");
        }
        plan.activate(current.get().getUsername());
        audit.record(current.organizationCode(), current.get().getUsername(), "QUOTA", "ACTIVATE", "QUOTA_PLAN", id,
            QuotaPlan.Status.DRAFT, QuotaPlan.Status.ACTIVE);
        return detail(id);
    }

    @Transactional
    public PlanDetail expire(Long id) {
        QuotaPlan plan = scopedPlan(id);
        plan.expire();
        audit.record(current.organizationCode(), current.get().getUsername(), "QUOTA", "EXPIRE", "QUOTA_PLAN", id, null, plan.getStatus());
        return detail(id);
    }

    @Transactional
    public ConsumptionResult consume(String organizationCode, String procurementOrganization, String factoryCode,
                                     String materialCode, LocalDate businessDate, Long supplierId, BigDecimal quantity,
                                     QuotaUsage.SourceType sourceType, String sourceId, String actor) {
        List<QuotaPlan> active = plans.findByOrganizationCodeAndProcurementOrganizationAndFactoryCodeAndMaterialCodeAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqualAndDeletedFalse(
            organizationCode, procurementOrganization, factoryCode, materialCode, QuotaPlan.Status.ACTIVE, businessDate, businessDate);
        if (active.isEmpty()) return new ConsumptionResult(false, true, "未找到生效配额方案", null, null, null);
        if (active.size() > 1) throw new IllegalStateException("检测到多个重叠的生效配额方案");
        QuotaPlan plan = active.getFirst();
        QuotaAllocation allocation = allocations.findByPlanIdAndSupplierIdAndDeletedFalse(plan.getId(), supplierId)
            .orElse(null);
        if (allocation == null) return new ConsumptionResult(false, true, "供应商未获得该物料配额", plan.getId(), null, null);
        if (usages.existsByAllocationIdAndSourceTypeAndSourceIdAndDeletedFalse(allocation.getId(), sourceType, sourceId)) {
            return new ConsumptionResult(false, allocation.isOverAllocated(), "该来源已计入配额，未重复扣减", plan.getId(), allocation.getUsedQuantity(), allocation.remainingQuantity());
        }
        allocation.consume(quantity);
        usages.save(new QuotaUsage(allocation, sourceType, sourceId, quantity, actor, null));
        boolean insufficient = allocation.remainingQuantity().signum() < 0;
        audit.record(organizationCode, actor, "QUOTA", "CONSUME", "QUOTA_ALLOCATION", allocation.getId(), null,
            Map.of("sourceId", sourceId, "quantity", quantity, "remaining", allocation.remainingQuantity()));
        return new ConsumptionResult(true, insufficient, insufficient ? "配额不足，已记录超配" : "配额已扣减",
            plan.getId(), allocation.getUsedQuantity(), allocation.remainingQuantity());
    }

    /** 将订单行来源同步到目标确认数量，支持订单变更后的增量、减量和跨配额版本迁移。 */
    @Transactional
    public ConsumptionResult synchronizeOrderUsage(String organizationCode, String procurementOrganization,
        String factoryCode, String materialCode, LocalDate businessDate, Long supplierId,
        BigDecimal desiredQuantity, String sourceId, String actor) {
        if (desiredQuantity.signum() < 0) throw new BusinessException("配额同步数量不能小于零");
        List<QuotaUsage> existingValues = usages.findBySourceTypeAndSourceIdAndDeletedFalse(
            QuotaUsage.SourceType.PURCHASE_ORDER, sourceId);
        if (existingValues.size() > 1) throw new IllegalStateException("同一订单行存在多条有效配额使用记录");
        QuotaUsage existing = existingValues.isEmpty() ? null : existingValues.getFirst();

        List<QuotaPlan> active = plans.findByOrganizationCodeAndProcurementOrganizationAndFactoryCodeAndMaterialCodeAndStatusAndValidFromLessThanEqualAndValidToGreaterThanEqualAndDeletedFalse(
            organizationCode, procurementOrganization, factoryCode, materialCode, QuotaPlan.Status.ACTIVE,
            businessDate, businessDate);
        if (active.size() > 1) throw new IllegalStateException("检测到多个重叠的生效配额方案");
        QuotaPlan plan = active.isEmpty() ? null : active.getFirst();
        QuotaAllocation target = plan == null ? null
            : allocations.findByPlanIdAndSupplierIdAndDeletedFalse(plan.getId(), supplierId).orElse(null);

        if (desiredQuantity.signum() > 0 && plan == null) {
            return new ConsumptionResult(false, true, "未找到生效配额方案", null, null, null);
        }
        if (desiredQuantity.signum() > 0 && target == null) {
            return new ConsumptionResult(false, true, "供应商未获得该物料配额", plan.getId(), null, null);
        }

        if (existing != null && (target == null || !existing.getAllocation().getId().equals(target.getId()))) {
            existing.getAllocation().adjustUsage(existing.getQuantity().negate());
            existing.softDelete();
            existing = null;
        }
        if (target == null) {
            audit.record(organizationCode, actor, "QUOTA", "ORDER_USAGE_CLEAR", "QUOTA_USAGE", sourceId,
                null, Map.of("quantity", BigDecimal.ZERO));
            return new ConsumptionResult(true, false, "订单行配额占用已清零", null, BigDecimal.ZERO, null);
        }

        BigDecimal previous = existing == null ? BigDecimal.ZERO : existing.getQuantity();
        BigDecimal delta = desiredQuantity.subtract(previous);
        if (delta.signum() != 0) target.adjustUsage(delta);
        if (existing == null) {
            usages.save(new QuotaUsage(target, QuotaUsage.SourceType.PURCHASE_ORDER, sourceId,
                desiredQuantity, actor, "订单确认同步"));
        } else {
            existing.adjust(desiredQuantity, actor, "订单确认或变更同步");
        }
        boolean insufficient = target.remainingQuantity().signum() < 0;
        audit.record(organizationCode, actor, "QUOTA", "ORDER_USAGE_SYNC", "QUOTA_ALLOCATION", target.getId(),
            Map.of("sourceId", sourceId, "quantity", previous),
            Map.of("sourceId", sourceId, "quantity", desiredQuantity, "remaining", target.remainingQuantity()));
        return new ConsumptionResult(delta.signum() != 0, insufficient,
            insufficient ? "配额不足，已记录超配" : delta.signum() == 0 ? "确认数量未变化，未重复扣减配额" : "配额已同步",
            plan.getId(), target.getUsedQuantity(), target.remainingQuantity());
    }

    @Transactional
    public ConsumptionResult manualConsume(Long planId, ManualUsageRequest request) {
        QuotaPlan plan = scopedPlan(planId);
        if (plan.getStatus() != QuotaPlan.Status.ACTIVE) throw new BusinessException("配额方案未生效");
        return consume(plan.getOrganizationCode(), plan.getProcurementOrganization(), plan.getFactoryCode(),
            plan.getMaterialCode(), request.businessDate(), request.supplierId(), request.quantity(),
            QuotaUsage.SourceType.MANUAL, request.sourceId(), current.get().getUsername());
    }

    public byte[] export() {
        List<QuotaPlan> data = plans.findByOrganizationCodeAndDeletedFalse(current.organizationCode(), Pageable.unpaged()).getContent();
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("配额执行");
            String[] headers = {"方案ID", "采购组织", "工厂", "物料编码", "物料名称", "类型", "版本", "状态", "供应商编码", "供应商名称", "分配值", "已分配数量", "已使用数量", "剩余数量", "预警"};
            var header = sheet.createRow(0); CellStyle style = workbook.createCellStyle();
            style.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex()); style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            var font = workbook.createFont(); font.setColor(IndexedColors.WHITE.getIndex()); font.setBold(true); style.setFont(font);
            for (int i = 0; i < headers.length; i++) { header.createCell(i).setCellValue(headers[i]); header.getCell(i).setCellStyle(style); }
            int rowIndex = 1;
            for (QuotaPlan plan : data) for (QuotaAllocation value : allocations.findByPlanIdAndDeletedFalse(plan.getId())) {
                var row = sheet.createRow(rowIndex++); int c = 0;
                row.createCell(c++).setCellValue(plan.getId()); row.createCell(c++).setCellValue(plan.getProcurementOrganization());
                row.createCell(c++).setCellValue(plan.getFactoryCode()); row.createCell(c++).setCellValue(plan.getMaterialCode());
                row.createCell(c++).setCellValue(plan.getMaterialName()); row.createCell(c++).setCellValue(plan.getQuotaType().name());
                row.createCell(c++).setCellValue(plan.getPlanVersion()); row.createCell(c++).setCellValue(plan.getStatus().name());
                row.createCell(c++).setCellValue(value.getSupplier().getSupplierCode()); row.createCell(c++).setCellValue(value.getSupplier().getSupplierName());
                row.createCell(c++).setCellValue(value.getQuotaValue().doubleValue()); row.createCell(c++).setCellValue(value.getAllocatedQuantity().doubleValue());
                row.createCell(c++).setCellValue(value.getUsedQuantity().doubleValue()); row.createCell(c++).setCellValue(value.remainingQuantity().doubleValue());
                row.createCell(c).setCellValue(value.isOverAllocated() ? "超配" : value.remainingQuantity().signum() == 0 ? "已用完" : "正常");
            }
            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);
            workbook.write(output); return output.toByteArray();
        } catch (IOException exception) { throw new BusinessException("配额导出失败"); }
    }

    private void validateAllocationTotal(QuotaPlan plan, List<QuotaAllocation> values) {
        if (values.isEmpty()) throw new BusinessException("至少需要一条供应商配额");
        BigDecimal total = values.stream().map(QuotaAllocation::getQuotaValue).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal expected = plan.getQuotaType() == QuotaPlan.Type.RATIO ? new BigDecimal("100") : plan.getTotalQuantity();
        if (total.compareTo(expected) != 0) throw new BusinessException(plan.getQuotaType() == QuotaPlan.Type.RATIO
            ? "比例配额合计必须等于 100" : "数量配额合计必须等于计划总数量");
    }
    private boolean overlaps(QuotaPlan left, QuotaPlan right) { return !left.getValidTo().isBefore(right.getValidFrom()) && !right.getValidTo().isBefore(left.getValidFrom()); }
    private List<String> warnings(QuotaPlan plan) {
        List<String> result = new ArrayList<>();
        for (QuotaAllocation allocation : allocations.findByPlanIdAndDeletedFalse(plan.getId())) {
            if (allocation.isOverAllocated()) result.add(allocation.getSupplier().getSupplierName() + " 已超配 " + allocation.remainingQuantity().abs());
            else if (allocation.remainingQuantity().signum() == 0) result.add(allocation.getSupplier().getSupplierName() + " 配额已用完");
        }
        return result;
    }
    private QuotaPlan scopedPlan(Long id) { return plans.findByIdAndOrganizationCodeAndDeletedFalse(id, current.organizationCode()).orElseThrow(() -> new BusinessException("配额方案不存在")); }
    private PlanView view(QuotaPlan plan) { return new PlanView(plan.getId(), plan.getProcurementOrganization(), plan.getFactoryCode(), plan.getMaterialCode(), plan.getMaterialName(), plan.getQuotaType(), plan.getTotalQuantity(), plan.getValidFrom(), plan.getValidTo(), plan.getPlanVersion(), plan.getStatus(), plan.getActivatedBy(), plan.getActivatedAt()); }

    public record AllocationRequest(@NotNull Long supplierId, @NotNull @DecimalMin("0.0001") BigDecimal value, String note) {}
    public record CreatePlanRequest(@NotBlank String procurementOrganization, @NotBlank String factoryCode,
        @NotBlank String materialCode, @NotBlank String materialName, @NotNull QuotaPlan.Type quotaType,
        @NotNull @DecimalMin("0.0001") BigDecimal totalQuantity, @NotNull LocalDate validFrom, @NotNull LocalDate validTo,
        @NotEmpty List<@Valid AllocationRequest> allocations, @NotBlank String changeReason) {}
    public record ManualUsageRequest(@NotNull Long supplierId, @NotNull @DecimalMin("0.0001") BigDecimal quantity,
        @NotNull LocalDate businessDate, @NotBlank String sourceId) {}
    public record PlanView(Long id, String procurementOrganization, String factoryCode, String materialCode,
        String materialName, QuotaPlan.Type quotaType, BigDecimal totalQuantity, LocalDate validFrom, LocalDate validTo,
        int version, QuotaPlan.Status status, String activatedBy, java.time.LocalDateTime activatedAt) {}
    public record AllocationView(Long id, Long supplierId, String supplierCode, String supplierName,
        BigDecimal quotaValue, BigDecimal allocatedQuantity, BigDecimal usedQuantity, BigDecimal remainingQuantity,
        boolean overAllocated, String note) {
        static AllocationView from(QuotaAllocation value) { return new AllocationView(value.getId(), value.getSupplier().getId(), value.getSupplier().getSupplierCode(), value.getSupplier().getSupplierName(), value.getQuotaValue(), value.getAllocatedQuantity(), value.getUsedQuantity(), value.remainingQuantity(), value.isOverAllocated(), value.getAdjustmentNote()); }
    }
    public record PlanDetail(PlanView plan, List<AllocationView> allocations, List<String> warnings) {}
    public record ConsumptionResult(boolean consumed, boolean warning, String message, Long planId,
        BigDecimal usedQuantity, BigDecimal remainingQuantity) {}
}
