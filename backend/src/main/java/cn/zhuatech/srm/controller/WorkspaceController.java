package cn.zhuatech.srm.controller;
import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.WorkspaceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
@RestController
public class WorkspaceController {
    private final WorkspaceService service; public WorkspaceController(WorkspaceService service) { this.service = service; }
    @GetMapping("/api/admin/dashboard") @PreAuthorize("@permissionService.has('DASHBOARD')")
    public ApiResponse<WorkspaceService.Dashboard> admin() { return ApiResponse.ok(service.adminDashboard()); }
    @GetMapping("/api/portal/dashboard") @PreAuthorize("@permissionService.has('PORTAL')")
    public ApiResponse<WorkspaceService.Dashboard> portal() { return ApiResponse.ok(service.supplierDashboard()); }
    @GetMapping("/api/inbox") @PreAuthorize("@permissionService.has('MESSAGE')")
    public ApiResponse<List<WorkspaceService.InboxView>> inbox() { return ApiResponse.ok(service.inbox()); }
    @PostMapping("/api/inbox/{id}/{action}") @PreAuthorize("@permissionService.has('MESSAGE')")
    public ApiResponse<WorkspaceService.InboxView> handle(@PathVariable Long id, @PathVariable String action) { return ApiResponse.ok(service.handleInbox(id, action)); }
    @GetMapping("/api/admin/audit-logs") @PreAuthorize("@permissionService.has('AUDIT_READ')")
    public ApiResponse<Page<WorkspaceService.AuditView>> audit(@RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String actor,
        @RequestParam(required = false) String module,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromTime,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toTime) {
        return ApiResponse.ok(service.audit(actor, module, fromTime, toTime, PageRequest.of(page, Math.min(size, 100))));
    }
}
