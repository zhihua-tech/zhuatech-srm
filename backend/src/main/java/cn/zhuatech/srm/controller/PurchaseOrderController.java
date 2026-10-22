/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.controller;

import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.PurchaseOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
public class PurchaseOrderController {
    private final PurchaseOrderService service;
    public PurchaseOrderController(PurchaseOrderService service) { this.service = service; }
    @GetMapping("/api/admin/orders") @PreAuthorize("@permissionService.has('ORDER_MANAGE') or @permissionService.has('ORDER_READ')")
    public ApiResponse<Page<PurchaseOrderService.OrderView>> adminList(@RequestParam(defaultValue = "0") int page,
                                                                       @RequestParam(defaultValue = "20") int size) { return ApiResponse.ok(service.adminList(page, size)); }
    @PostMapping("/api/admin/orders") @PreAuthorize("@permissionService.has('ORDER_MANAGE')")
    public ApiResponse<PurchaseOrderService.OrderDetail> create(@Valid @RequestBody PurchaseOrderService.OpenApiOrderRequest request) { return ApiResponse.ok(service.create(request)); }
    @PostMapping(value = "/api/admin/orders/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("@permissionService.has('IMPORT')")
    public ApiResponse<PurchaseOrderService.ImportResult> importExcel(@RequestPart("file") MultipartFile file) { return ApiResponse.ok(service.importExcel(file)); }
    @GetMapping("/api/admin/orders/import-template") @PreAuthorize("@permissionService.has('IMPORT')")
    public ResponseEntity<byte[]> importTemplate() {
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchase-order-import-template.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(service.importTemplate());
    }
    @PostMapping("/api/admin/orders/{id}/versions") @PreAuthorize("@permissionService.has('ORDER_MANAGE')")
    public ApiResponse<PurchaseOrderService.OrderDetail> change(@PathVariable Long id, @Valid @RequestBody PurchaseOrderService.ChangeOrderRequest request) { return ApiResponse.ok(service.change(id, request)); }
    @GetMapping("/api/portal/orders/list") @PreAuthorize("@permissionService.has('PORTAL')")
    public ApiResponse<List<PurchaseOrderService.OrderView>> portalList() { return ApiResponse.ok(service.portalList()); }
    @GetMapping({"/api/admin/orders/{id}", "/api/portal/orders/{id}"})
    @PreAuthorize("@permissionService.has('ORDER_MANAGE') or @permissionService.has('ORDER_READ') or @permissionService.has('ORDER_CONFIRM')")
    public ApiResponse<PurchaseOrderService.OrderDetail> detail(@PathVariable Long id) { return ApiResponse.ok(service.detail(id)); }
    @PostMapping("/api/portal/orders/{orderId}/lines/{lineId}/confirm") @PreAuthorize("@permissionService.has('ORDER_CONFIRM')")
    public ApiResponse<PurchaseOrderService.OrderDetail> confirm(@PathVariable Long orderId, @PathVariable Long lineId,
                                                                 @Valid @RequestBody PurchaseOrderService.ConfirmLineRequest request) {
        return ApiResponse.ok(service.confirmLine(orderId, lineId, request));
    }
    @PostMapping("/api/portal/orders/{orderId}/confirm") @PreAuthorize("@permissionService.has('ORDER_CONFIRM')")
    public ApiResponse<PurchaseOrderService.OrderDetail> confirmOrder(@PathVariable Long orderId,
        @Valid @RequestBody PurchaseOrderService.ConfirmOrderRequest request) {
        return ApiResponse.ok(service.confirmOrder(orderId, request));
    }
}
