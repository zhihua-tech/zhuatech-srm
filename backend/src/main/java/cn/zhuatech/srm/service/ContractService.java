/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.ContractRecord;
import cn.zhuatech.srm.model.PurchaseOrder;
import cn.zhuatech.srm.model.Supplier;
import cn.zhuatech.srm.repository.ContractRepository;
import cn.zhuatech.srm.repository.PurchaseOrderRepository;
import cn.zhuatech.srm.repository.SupplierRepository;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** 合同台账、审批、不可覆盖版本、到期提醒与订单履约金额。 */
@Service
@Transactional(readOnly = true)
public class ContractService {
    private final ContractRepository contracts;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository orders;
    private final CurrentUserService current;
    private final AuditService audit;

    public ContractService(ContractRepository contracts, SupplierRepository suppliers, PurchaseOrderRepository orders,
                           CurrentUserService current, AuditService audit) {
        this.contracts = contracts;
        this.suppliers = suppliers;
        this.orders = orders;
        this.current = current;
        this.audit = audit;
    }

    public List<ContractView> list() {
        List<ContractRecord> source = current.isSupplier()
            ? contracts.findBySupplierIdAndDeletedFalse(current.requireSupplier().getId())
            : contracts.findByOrganizationCodeAndDeletedFalse(current.organizationCode());
        return source.stream().map(this::view).toList();
    }

    public ContractDetail detail(Long id) {
        ContractRecord selected = scoped(id);
        List<ContractView> history = contracts
            .findByOrganizationCodeAndContractNoAndDeletedFalseOrderByContractVersionDesc(
                selected.getOrganizationCode(), selected.getContractNo())
            .stream()
            .filter(value -> !current.isSupplier()
                || value.getSupplier().getId().equals(current.requireSupplier().getId()))
            .map(this::view)
            .toList();
        return new ContractDetail(view(selected), history);
    }

    public List<ContractView> expiring(int days) {
        LocalDate today = LocalDate.now();
        return list().stream()
            .filter(value -> value.status() == ContractRecord.Status.ACTIVE)
            .filter(value -> !value.effectiveTo().isBefore(today)
                && !value.effectiveTo().isAfter(today.plusDays(days)))
            .toList();
    }

    @Transactional
    public ContractView create(CreateRequest request) {
        validateDates(request.effectiveFrom(), request.effectiveTo());
        if (contracts.countByOrganizationCodeAndContractNoAndDeletedFalse(
            current.organizationCode(), request.contractNo()) > 0) {
            throw new BusinessException("合同编号已存在，请从原合同创建新版本");
        }
        Supplier supplier = scopedSupplier(request.supplierId());
        ContractRecord record = contracts.save(new ContractRecord(current.organizationCode(), request.contractNo(), 1,
            supplier, request.title(), request.amount(), request.currency(), request.effectiveFrom(), request.effectiveTo(),
            request.attachmentKey(), request.changeReason()));
        audit.record(current.organizationCode(), current.get().getUsername(), "CONTRACT", "CREATE", "CONTRACT",
            record.getId(), null, Map.of("contractNo", record.getContractNo(), "version", 1));
        return view(record);
    }

    @Transactional
    public ContractView revise(Long sourceId, RevisionRequest request) {
        ContractRecord source = scoped(sourceId);
        List<ContractRecord> history = contracts
            .findByOrganizationCodeAndContractNoAndDeletedFalseOrderByContractVersionDesc(
                source.getOrganizationCode(), source.getContractNo());
        if (history.isEmpty() || !history.getFirst().getId().equals(sourceId)) {
            throw new BusinessException("只能基于合同最新版本创建变更版本");
        }
        if (source.getStatus() == ContractRecord.Status.DRAFT
            || source.getStatus() == ContractRecord.Status.PENDING_APPROVAL) {
            throw new BusinessException("当前版本尚未完成审批，不能创建新版本");
        }
        validateDates(request.effectiveFrom(), request.effectiveTo());
        ContractRecord next = contracts.save(new ContractRecord(source.getOrganizationCode(), source.getContractNo(),
            source.getContractVersion() + 1, source.getSupplier(), request.title(), request.amount(), request.currency(),
            request.effectiveFrom(), request.effectiveTo(), request.attachmentKey(), request.changeReason()));
        audit.record(current.organizationCode(), current.get().getUsername(), "CONTRACT", "CREATE_VERSION", "CONTRACT",
            next.getId(), Map.of("sourceId", sourceId, "version", source.getContractVersion()),
            Map.of("version", next.getContractVersion(), "reason", request.changeReason()));
        return view(next);
    }

    @Transactional
    public ContractView transition(Long id, String action) {
        ContractRecord value = scoped(id);
        ContractRecord.Status before = value.getStatus();
        try {
            switch (action.toUpperCase()) {
                case "SUBMIT" -> value.submit();
                case "APPROVE" -> value.approve();
                case "ACTIVATE" -> activateVersion(value);
                case "EXPIRE" -> value.expire();
                case "TERMINATE" -> value.terminate();
                default -> throw new BusinessException("不支持的合同动作");
            }
        } catch (IllegalStateException exception) {
            throw new BusinessException(exception.getMessage());
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "CONTRACT", action.toUpperCase(),
            "CONTRACT", id, before, value.getStatus());
        return view(value);
    }

    private void activateVersion(ContractRecord value) {
        for (ContractRecord previous : contracts
            .findByOrganizationCodeAndContractNoAndDeletedFalseOrderByContractVersionDesc(
                value.getOrganizationCode(), value.getContractNo())) {
            if (!previous.getId().equals(value.getId())
                && previous.getStatus() == ContractRecord.Status.ACTIVE) previous.supersede();
        }
        value.activate();
    }

    private ContractRecord scoped(Long id) {
        ContractRecord value = contracts.findById(id).filter(record -> !record.isDeleted())
            .orElseThrow(() -> new BusinessException("合同不存在"));
        if (!value.getOrganizationCode().equals(current.organizationCode())) throw new BusinessException("无权访问该合同");
        if (current.isSupplier()
            && !value.getSupplier().getId().equals(current.requireSupplier().getId())) {
            throw new BusinessException("无权访问其他供应商合同");
        }
        return value;
    }

    private Supplier scopedSupplier(Long id) {
        return suppliers.findByIdAndOrganizationCodeAndDeletedFalse(id, current.organizationCode())
            .orElseThrow(() -> new BusinessException("供应商不存在"));
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw new BusinessException("合同结束日期不能早于开始日期");
    }

    private ContractView view(ContractRecord value) {
        BigDecimal executionAmount = orders
            .findBySupplierIdAndDeliveryDateBetweenAndDeletedFalse(
                value.getSupplier().getId(), value.getEffectiveFrom(), value.getEffectiveTo())
            .stream()
            .filter(order -> order.getStatus() != PurchaseOrder.Status.CANCELLED)
            .map(PurchaseOrder::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal executionRate = value.getAmount().signum() == 0 ? BigDecimal.ZERO
            : executionAmount.multiply(new BigDecimal("100"))
                .divide(value.getAmount(), 2, RoundingMode.HALF_UP);
        String fulfillment = executionAmount.compareTo(value.getAmount()) > 0 ? "OVER_AMOUNT"
            : value.getEffectiveTo().isBefore(LocalDate.now()) ? "EXPIRED"
            : executionRate.compareTo(new BigDecimal("80")) >= 0 ? "ON_TRACK" : "IN_PROGRESS";
        return new ContractView(value.getId(), value.getContractNo(), value.getContractVersion(),
            value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getTitle(), value.getAmount(),
            value.getCurrency(), value.getEffectiveFrom(), value.getEffectiveTo(), value.getAttachmentKey(),
            value.getStatus(), value.getChangeReason(), executionAmount, executionRate, fulfillment);
    }

    public record CreateRequest(@NotBlank String contractNo, @NotNull Long supplierId, @NotBlank String title,
        @NotNull @DecimalMin("0") BigDecimal amount, @NotBlank String currency, @NotNull LocalDate effectiveFrom,
        @NotNull LocalDate effectiveTo, String attachmentKey, @NotBlank String changeReason) {}
    public record RevisionRequest(@NotBlank String title, @NotNull @DecimalMin("0") BigDecimal amount,
        @NotBlank String currency, @NotNull LocalDate effectiveFrom, @NotNull LocalDate effectiveTo,
        String attachmentKey, @NotBlank String changeReason) {}
    public record ContractView(Long id, String contractNo, int version, Long supplierId, String supplierName,
        String title, BigDecimal amount, String currency, LocalDate effectiveFrom, LocalDate effectiveTo,
        String attachmentKey, ContractRecord.Status status, String changeReason, BigDecimal executionAmount,
        BigDecimal executionRate, String fulfillmentStatus) {}
    public record ContractDetail(ContractView contract, List<ContractView> versionHistory) {}
}
