/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.controller;

import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.QuotaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/admin/quotas")
public class QuotaController {
    private final QuotaService service;
    public QuotaController(QuotaService service) { this.service = service; }
    @GetMapping @PreAuthorize("@permissionService.has('QUOTA_MANAGE') or @permissionService.has('QUOTA_READ')")
    public ApiResponse<Page<QuotaService.PlanView>> list(@RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "20") int size) { return ApiResponse.ok(service.list(page, size)); }
    @GetMapping("/{id}") @PreAuthorize("@permissionService.has('QUOTA_MANAGE') or @permissionService.has('QUOTA_READ')")
    public ApiResponse<QuotaService.PlanDetail> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping @PreAuthorize("@permissionService.has('QUOTA_MANAGE')")
    public ApiResponse<QuotaService.PlanDetail> create(@Valid @RequestBody QuotaService.CreatePlanRequest request) { return ApiResponse.ok(service.create(request)); }
    @PostMapping("/{id}/activate") @PreAuthorize("@permissionService.has('QUOTA_MANAGE')")
    public ApiResponse<QuotaService.PlanDetail> activate(@PathVariable Long id) { return ApiResponse.ok(service.activate(id)); }
    @PostMapping("/{id}/expire") @PreAuthorize("@permissionService.has('QUOTA_MANAGE')")
    public ApiResponse<QuotaService.PlanDetail> expire(@PathVariable Long id) { return ApiResponse.ok(service.expire(id)); }
    @PostMapping("/{id}/manual-usage") @PreAuthorize("@permissionService.has('QUOTA_MANAGE')")
    public ApiResponse<QuotaService.ConsumptionResult> consume(@PathVariable Long id, @Valid @RequestBody QuotaService.ManualUsageRequest request) { return ApiResponse.ok(service.manualConsume(id, request)); }
    @GetMapping("/export") @PreAuthorize("@permissionService.has('EXPORT')")
    public ResponseEntity<byte[]> export() {
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=quota-execution.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(service.export());
    }
}
