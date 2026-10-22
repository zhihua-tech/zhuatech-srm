/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.controller;

import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.SourcingService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class SourcingController {
    private final SourcingService service;
    public SourcingController(SourcingService service) { this.service = service; }
    @GetMapping("/api/admin/sourcing-events") @PreAuthorize("@permissionService.has('SOURCING_MANAGE') or @permissionService.has('SOURCING_READ')")
    public ApiResponse<List<SourcingService.EventView>> adminList() { return ApiResponse.ok(service.adminList()); }
    @PostMapping("/api/admin/sourcing-events") @PreAuthorize("@permissionService.has('SOURCING_MANAGE')")
    public ApiResponse<SourcingService.EventDetail> create(@Valid @RequestBody SourcingService.CreateEventRequest request) { return ApiResponse.ok(service.create(request)); }
    @PostMapping("/api/admin/sourcing-events/{id}/publish") @PreAuthorize("@permissionService.has('SOURCING_MANAGE')")
    public ApiResponse<SourcingService.EventView> publish(@PathVariable Long id) { return ApiResponse.ok(service.publish(id)); }
    @GetMapping("/api/admin/sourcing-events/{id}/compare") @PreAuthorize("@permissionService.has('SOURCING_MANAGE') or @permissionService.has('SOURCING_READ')")
    public ApiResponse<List<SourcingService.QuoteView>> compare(@PathVariable Long id) { return ApiResponse.ok(service.compare(id)); }
    @PostMapping("/api/admin/sourcing-events/{eventId}/quotes/{quoteId}/evaluate") @PreAuthorize("@permissionService.has('SOURCING_MANAGE')")
    public ApiResponse<SourcingService.QuoteView> evaluate(@PathVariable Long eventId, @PathVariable Long quoteId,
        @Valid @RequestBody SourcingService.EvaluationRequest request) { return ApiResponse.ok(service.evaluate(eventId, quoteId, request)); }
    @PostMapping("/api/admin/sourcing-events/{id}/award") @PreAuthorize("@permissionService.has('SOURCING_MANAGE')")
    public ApiResponse<SourcingService.EventView> award(@PathVariable Long id, @Valid @RequestBody SourcingService.AwardRequest request) { return ApiResponse.ok(service.requestAward(id, request)); }
    @PostMapping("/api/admin/sourcing-events/{id}/award/approval") @PreAuthorize("@permissionService.has('SOURCING_MANAGE')")
    public ApiResponse<SourcingService.EventView> approveAward(@PathVariable Long id,
        @Valid @RequestBody SourcingService.AwardApprovalRequest request) { return ApiResponse.ok(service.approveAward(id, request)); }
    @PostMapping("/api/admin/clarifications/{id}/answer") @PreAuthorize("@permissionService.has('SOURCING_MANAGE')")
    public ApiResponse<SourcingService.ClarificationView> answer(@PathVariable Long id, @Valid @RequestBody SourcingService.ClarificationRequest request) { return ApiResponse.ok(service.answer(id, request)); }
    @GetMapping("/api/portal/sourcing-events") @PreAuthorize("@permissionService.has('PORTAL')")
    public ApiResponse<List<SourcingService.EventView>> portalList() { return ApiResponse.ok(service.portalList()); }
    @GetMapping("/api/portal/sourcing-events/{id}") @PreAuthorize("@permissionService.has('PORTAL')")
    public ApiResponse<SourcingService.EventDetail> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping("/api/portal/sourcing-events/{id}/quotes") @PreAuthorize("@permissionService.has('QUOTE_SUBMIT')")
    public ApiResponse<SourcingService.QuoteView> quote(@PathVariable Long id, @Valid @RequestBody SourcingService.QuoteRequest request) { return ApiResponse.ok(service.submitQuote(id, request)); }
    @PostMapping("/api/portal/sourcing-events/{id}/clarifications") @PreAuthorize("@permissionService.has('QUOTE_SUBMIT')")
    public ApiResponse<SourcingService.ClarificationView> ask(@PathVariable Long id, @Valid @RequestBody SourcingService.ClarificationRequest request) { return ApiResponse.ok(service.ask(id, request)); }
    @GetMapping("/api/portal/sourcing-events/{id}/clarifications") @PreAuthorize("@permissionService.has('PORTAL')")
    public ApiResponse<List<SourcingService.ClarificationView>> clarifications(@PathVariable Long id) { return ApiResponse.ok(service.portalClarifications(id)); }
}
