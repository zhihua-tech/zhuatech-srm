package cn.zhuatech.srm.controller;
import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.AdministrationService;
import cn.zhuatech.srm.service.BusinessRuleService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/admin/system") @PreAuthorize("hasRole('ADMIN')")
public class AdministrationController {
    private final AdministrationService service; private final BusinessRuleService businessRules;
    public AdministrationController(AdministrationService service, BusinessRuleService businessRules){this.service=service;this.businessRules=businessRules;}
    @GetMapping("/organizations")public ApiResponse<List<AdministrationService.OrganizationView>> organizations(){return ApiResponse.ok(service.organizations());}
    @PostMapping("/organizations")public ApiResponse<AdministrationService.OrganizationView> createOrganization(@Valid @RequestBody AdministrationService.OrganizationRequest request){return ApiResponse.ok(service.createOrganization(request));}
    @GetMapping("/users")public ApiResponse<List<AdministrationService.UserView>> users(){return ApiResponse.ok(service.users());}
    @PostMapping("/users")public ApiResponse<AdministrationService.UserView> createUser(@Valid @RequestBody AdministrationService.UserRequest request){return ApiResponse.ok(service.createUser(request));}
    @PatchMapping("/users/{id}/enabled")public ApiResponse<AdministrationService.UserView> enabled(@PathVariable Long id,@RequestParam boolean enabled){return ApiResponse.ok(service.setEnabled(id,enabled));}
    @GetMapping("/roles/{role}/permissions")public ApiResponse<AdministrationService.RolePermissionView> rolePermissions(@PathVariable cn.zhuatech.srm.model.UserAccount.Role role){return ApiResponse.ok(service.rolePermissions(role));}
    @PutMapping("/roles/{role}/permissions")public ApiResponse<AdministrationService.RolePermissionView> replaceRolePermissions(@PathVariable cn.zhuatech.srm.model.UserAccount.Role role,@Valid @RequestBody AdministrationService.RolePermissionRequest request){return ApiResponse.ok(service.replaceRolePermissions(role,request));}
    @PostMapping("/business-rules/refresh")public ApiResponse<BusinessRuleService.RefreshResult> refreshBusinessRules(){return ApiResponse.ok(businessRules.refreshCurrentOrganization());}
}
