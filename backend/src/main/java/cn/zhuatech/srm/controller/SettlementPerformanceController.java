package cn.zhuatech.srm.controller;
import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.SettlementPerformanceService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
public class SettlementPerformanceController {
    private final SettlementPerformanceService service; public SettlementPerformanceController(SettlementPerformanceService service) { this.service = service; }
    @GetMapping("/api/reconciliations") @PreAuthorize("@permissionService.has('RECONCILIATION_MANAGE') or @permissionService.has('RECONCILIATION_READ') or @permissionService.has('RECONCILIATION_CONFIRM')")
    public ApiResponse<List<SettlementPerformanceService.StatementView>> statements() { return ApiResponse.ok(service.statements()); }
    @GetMapping("/api/reconciliations/{id}") @PreAuthorize("@permissionService.has('RECONCILIATION_MANAGE') or @permissionService.has('RECONCILIATION_READ') or @permissionService.has('RECONCILIATION_CONFIRM')")
    public ApiResponse<SettlementPerformanceService.StatementView> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping("/api/admin/reconciliations") @PreAuthorize("@permissionService.has('RECONCILIATION_MANAGE')")
    public ApiResponse<SettlementPerformanceService.StatementView> generate(@Valid @RequestBody SettlementPerformanceService.GenerateStatementRequest request) { return ApiResponse.ok(service.generate(request)); }
    @PostMapping("/api/portal/reconciliations/{id}/action") @PreAuthorize("@permissionService.has('RECONCILIATION_CONFIRM')")
    public ApiResponse<SettlementPerformanceService.StatementView> action(@PathVariable Long id, @Valid @RequestBody SettlementPerformanceService.StatementActionRequest request) { return ApiResponse.ok(service.supplierAction(id, request)); }
    @PostMapping("/api/portal/reconciliations/{id}/invoices") @PreAuthorize("@permissionService.has('RECONCILIATION_CONFIRM')")
    public ApiResponse<SettlementPerformanceService.InvoiceView> invoice(@PathVariable Long id, @Valid @RequestBody SettlementPerformanceService.InvoiceRequest request) { return ApiResponse.ok(service.registerInvoice(id, request)); }
    @GetMapping("/api/performance") @PreAuthorize("@permissionService.has('PERFORMANCE_MANAGE') or @permissionService.has('PERFORMANCE_READ') or @permissionService.has('PORTAL')")
    public ApiResponse<List<SettlementPerformanceService.PerformanceView>> performance() { return ApiResponse.ok(service.performance()); }
    @PostMapping("/api/admin/performance") @PreAuthorize("@permissionService.has('PERFORMANCE_MANAGE')")
    public ApiResponse<SettlementPerformanceService.PerformanceView> score(@Valid @RequestBody SettlementPerformanceService.PerformanceRequest request) { return ApiResponse.ok(service.score(request)); }
    @GetMapping("/api/risks") @PreAuthorize("@permissionService.has('PERFORMANCE_MANAGE') or @permissionService.has('PERFORMANCE_READ') or @permissionService.has('PORTAL')")
    public ApiResponse<List<SettlementPerformanceService.RiskView>> risks() { return ApiResponse.ok(service.risks()); }
    @PostMapping("/api/risks/{id}/action") @PreAuthorize("@permissionService.has('PERFORMANCE_MANAGE') or @permissionService.has('QUALITY_ACTION')")
    public ApiResponse<SettlementPerformanceService.RiskView> risk(@PathVariable Long id, @Valid @RequestBody SettlementPerformanceService.RiskActionRequest request) { return ApiResponse.ok(service.updateRisk(id, request)); }
}
