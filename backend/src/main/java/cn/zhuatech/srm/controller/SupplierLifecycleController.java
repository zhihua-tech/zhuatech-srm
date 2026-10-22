/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.controller;

import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.SupplierLifecycleService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class SupplierLifecycleController {
    private final SupplierLifecycleService service;
    public SupplierLifecycleController(SupplierLifecycleService service) { this.service = service; }

    @GetMapping("/api/admin/suppliers/page") @PreAuthorize("@permissionService.has('SUPPLIER_READ')")
    public ApiResponse<Page<SupplierLifecycleService.SupplierView>> list(@RequestParam(defaultValue = "0") int page,
                                                                         @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.ok(service.list(PageRequest.of(page, Math.min(size, 100))));
    }
    @GetMapping("/api/admin/suppliers/{id}") @PreAuthorize("@permissionService.has('SUPPLIER_READ')")
    public ApiResponse<SupplierLifecycleService.SupplierDetail> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping("/api/admin/supplier-invitations") @PreAuthorize("@permissionService.has('SUPPLIER_MANAGE')")
    public ApiResponse<SupplierLifecycleService.InvitationView> invite(@Valid @RequestBody SupplierLifecycleService.InviteRequest request) { return ApiResponse.ok(service.invite(request)); }
    @PostMapping("/api/public/suppliers/register")
    public ApiResponse<SupplierLifecycleService.SupplierView> register(@Valid @RequestBody SupplierLifecycleService.RegisterRequest request) { return ApiResponse.ok(service.register(request)); }
    @PutMapping("/api/admin/suppliers/{id}") @PreAuthorize("@permissionService.has('SUPPLIER_MANAGE')")
    public ApiResponse<SupplierLifecycleService.SupplierView> update(@PathVariable Long id, @RequestParam @jakarta.validation.constraints.NotBlank String reason,
                                                                      @Valid @RequestBody SupplierLifecycleService.ProfileRequest request) { return ApiResponse.ok(service.updateProfile(id, request, reason)); }
    @PostMapping("/api/admin/suppliers/{id}/contacts") @PreAuthorize("@permissionService.has('SUPPLIER_MANAGE')")
    public ApiResponse<SupplierLifecycleService.ContactView> contact(@PathVariable Long id, @Valid @RequestBody SupplierLifecycleService.ContactRequest request) { return ApiResponse.ok(service.addContact(id, request)); }
    @PostMapping("/api/portal/profile/contacts") @PreAuthorize("@permissionService.has('SUPPLIER_SELF_MANAGE')")
    public ApiResponse<SupplierLifecycleService.ContactView> selfContact(@Valid @RequestBody SupplierLifecycleService.ContactRequest request) {
        return ApiResponse.ok(service.addContact(service.self().supplier().id(), request));
    }
    @PostMapping("/api/admin/suppliers/{id}/certificates") @PreAuthorize("@permissionService.has('SUPPLIER_MANAGE')")
    public ApiResponse<SupplierLifecycleService.CertificateView> certificate(@PathVariable Long id, @Valid @RequestBody SupplierLifecycleService.CertificateRequest request) { return ApiResponse.ok(service.addCertificate(id, request)); }
    @PostMapping("/api/portal/profile/certificates") @PreAuthorize("@permissionService.has('SUPPLIER_SELF_MANAGE')")
    public ApiResponse<SupplierLifecycleService.CertificateView> selfCertificate(@Valid @RequestBody SupplierLifecycleService.CertificateRequest request) {
        return ApiResponse.ok(service.addCertificate(service.self().supplier().id(), request));
    }
    @PatchMapping("/api/admin/suppliers/{id}/status") @PreAuthorize("@permissionService.has('SUPPLIER_MANAGE')")
    public ApiResponse<SupplierLifecycleService.SupplierView> status(@PathVariable Long id, @Valid @RequestBody SupplierLifecycleService.StatusRequest request) { return ApiResponse.ok(service.changeStatus(id, request)); }
    @GetMapping("/api/portal/profile") @PreAuthorize("@permissionService.has('SUPPLIER_SELF_MANAGE')")
    public ApiResponse<SupplierLifecycleService.SelfProfileDetail> self() { return ApiResponse.ok(service.selfProfile()); }
    @PutMapping("/api/portal/profile") @PreAuthorize("@permissionService.has('SUPPLIER_SELF_MANAGE')")
    public ApiResponse<SupplierLifecycleService.SupplierView> updateSelf(@RequestParam @jakarta.validation.constraints.NotBlank String reason,
                                                                         @Valid @RequestBody SupplierLifecycleService.ProfileRequest request) {
        return ApiResponse.ok(service.updateProfile(service.self().supplier().id(), request, reason));
    }
    @PutMapping("/api/portal/onboarding/questionnaire") @PreAuthorize("@permissionService.has('SUPPLIER_SELF_MANAGE')")
    public ApiResponse<SupplierLifecycleService.OnboardingView> questionnaire(@RequestBody String body) { return ApiResponse.ok(service.saveQuestionnaire(body)); }
    @PostMapping("/api/portal/onboarding/submit") @PreAuthorize("@permissionService.has('SUPPLIER_SELF_MANAGE')")
    public ApiResponse<SupplierLifecycleService.OnboardingView> submit() { return ApiResponse.ok(service.submitOnboarding()); }
    @PostMapping("/api/admin/onboarding/{id}/approve") @PreAuthorize("@permissionService.has('SUPPLIER_MANAGE')")
    public ApiResponse<SupplierLifecycleService.OnboardingView> approve(@PathVariable Long id, @Valid @RequestBody SupplierLifecycleService.ApprovalRequest request) { return ApiResponse.ok(service.approveOnboarding(id, request)); }
}
