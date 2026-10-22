package cn.zhuatech.srm.controller;
import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.ContractService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController @RequestMapping("/api/contracts")
public class ContractController {
    private final ContractService service; public ContractController(ContractService service) { this.service = service; }
    @GetMapping @PreAuthorize("@permissionService.has('CONTRACT_MANAGE') or @permissionService.has('CONTRACT_READ') or @permissionService.has('PORTAL')")
    public ApiResponse<List<ContractService.ContractView>> list() { return ApiResponse.ok(service.list()); }
    @GetMapping("/expiring") @PreAuthorize("@permissionService.has('CONTRACT_MANAGE') or @permissionService.has('CONTRACT_READ')")
    public ApiResponse<List<ContractService.ContractView>> expiring(@RequestParam(defaultValue = "30") int days) { return ApiResponse.ok(service.expiring(Math.min(days, 365))); }
    @GetMapping("/{id}") @PreAuthorize("@permissionService.has('CONTRACT_MANAGE') or @permissionService.has('CONTRACT_READ') or @permissionService.has('PORTAL')")
    public ApiResponse<ContractService.ContractDetail> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping @PreAuthorize("@permissionService.has('CONTRACT_MANAGE')")
    public ApiResponse<ContractService.ContractView> create(@Valid @RequestBody ContractService.CreateRequest request) { return ApiResponse.ok(service.create(request)); }
    @PostMapping("/{id}/versions") @PreAuthorize("@permissionService.has('CONTRACT_MANAGE')")
    public ApiResponse<ContractService.ContractView> revise(@PathVariable Long id, @Valid @RequestBody ContractService.RevisionRequest request) { return ApiResponse.ok(service.revise(id, request)); }
    @PostMapping("/{id}/{action}") @PreAuthorize("@permissionService.has('CONTRACT_MANAGE')")
    public ApiResponse<ContractService.ContractView> transition(@PathVariable Long id, @PathVariable String action) { return ApiResponse.ok(service.transition(id, action.toUpperCase())); }
}
