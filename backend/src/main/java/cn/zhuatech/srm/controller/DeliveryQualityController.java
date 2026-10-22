package cn.zhuatech.srm.controller;
import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.DeliveryQualityService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.nio.charset.StandardCharsets;
import java.util.List;
@RestController
public class DeliveryQualityController {
    private final DeliveryQualityService service; public DeliveryQualityController(DeliveryQualityService service) { this.service = service; }
    @GetMapping("/api/asns") @PreAuthorize("@permissionService.has('ASN_MANAGE') or @permissionService.has('DELIVERY_READ') or @permissionService.has('ORDER_MANAGE')")
    public ApiResponse<List<DeliveryQualityService.AsnView>> asns() { return ApiResponse.ok(service.asnList()); }
    @GetMapping("/api/asns/{id}") @PreAuthorize("@permissionService.has('ASN_MANAGE') or @permissionService.has('DELIVERY_READ') or @permissionService.has('ORDER_MANAGE')")
    public ApiResponse<DeliveryQualityService.AsnDetail> asn(@PathVariable Long id) { return ApiResponse.ok(service.asnDetail(id)); }
    @PostMapping("/api/portal/asns") @PreAuthorize("@permissionService.has('ASN_MANAGE')")
    public ApiResponse<DeliveryQualityService.AsnDetail> createAsn(@Valid @RequestBody DeliveryQualityService.CreateAsnRequest request) { return ApiResponse.ok(service.createAsn(request)); }
    @PostMapping("/api/asns/{id}/appointment") @PreAuthorize("@permissionService.has('ASN_MANAGE') or @permissionService.has('ORDER_MANAGE')")
    public ApiResponse<DeliveryQualityService.AsnDetail> appoint(@PathVariable Long id, @Valid @RequestBody DeliveryQualityService.AppointmentRequest request) { return ApiResponse.ok(service.appoint(id, request)); }
    @PostMapping("/api/portal/asns/{id}/ship") @PreAuthorize("@permissionService.has('ASN_MANAGE')")
    public ApiResponse<DeliveryQualityService.AsnDetail> ship(@PathVariable Long id, @Valid @RequestBody DeliveryQualityService.ShipRequest request) { return ApiResponse.ok(service.ship(id, request)); }
    @PostMapping("/api/admin/asns/{id}/receive") @PreAuthorize("@permissionService.has('ORDER_MANAGE')")
    public ApiResponse<DeliveryQualityService.AsnDetail> receive(@PathVariable Long id, @Valid @RequestBody DeliveryQualityService.ReceiveRequest request) { return ApiResponse.ok(service.receive(id, request)); }
    @GetMapping("/api/quality-issues") @PreAuthorize("@permissionService.has('QUALITY_MANAGE') or @permissionService.has('QUALITY_READ') or @permissionService.has('QUALITY_ACTION')")
    public ApiResponse<List<DeliveryQualityService.QualityView>> quality() { return ApiResponse.ok(service.qualityList()); }
    @GetMapping("/api/quality-issues/{id}") @PreAuthorize("@permissionService.has('QUALITY_MANAGE') or @permissionService.has('QUALITY_READ') or @permissionService.has('QUALITY_ACTION')")
    public ApiResponse<DeliveryQualityService.QualityDetail> qualityDetail(@PathVariable Long id) { return ApiResponse.ok(service.qualityDetail(id)); }
    @GetMapping("/api/admin/quality-issues/export") @PreAuthorize("@permissionService.has('QUALITY_MANAGE') and @permissionService.has('EXPORT')")
    public ResponseEntity<byte[]> exportQuality() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.attachment().filename("quality-issues.xlsx", StandardCharsets.UTF_8).build());
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        return ResponseEntity.ok().headers(headers).body(service.exportQuality());
    }
    @PostMapping("/api/admin/quality-issues") @PreAuthorize("@permissionService.has('QUALITY_MANAGE')")
    public ApiResponse<DeliveryQualityService.QualityView> createQuality(@Valid @RequestBody DeliveryQualityService.CreateQualityRequest request) { return ApiResponse.ok(service.createQuality(request)); }
    @PostMapping("/api/portal/quality-issues/{id}/start") @PreAuthorize("@permissionService.has('QUALITY_ACTION')")
    public ApiResponse<DeliveryQualityService.QualityView> start(@PathVariable Long id) { return ApiResponse.ok(service.startAnalysis(id)); }
    @PostMapping("/api/portal/quality-issues/{id}/actions") @PreAuthorize("@permissionService.has('QUALITY_ACTION')")
    public ApiResponse<DeliveryQualityService.QualityView> submitAction(@PathVariable Long id, @Valid @RequestBody DeliveryQualityService.CorrectiveActionRequest request) { return ApiResponse.ok(service.submitAction(id, request)); }
    @PostMapping("/api/admin/quality-issues/{id}/review") @PreAuthorize("@permissionService.has('QUALITY_MANAGE')")
    public ApiResponse<DeliveryQualityService.QualityView> review(@PathVariable Long id, @Valid @RequestBody DeliveryQualityService.ReviewActionRequest request) { return ApiResponse.ok(service.reviewAction(id, request)); }
    @PostMapping("/api/admin/quality-issues/{id}/close") @PreAuthorize("@permissionService.has('QUALITY_MANAGE')")
    public ApiResponse<DeliveryQualityService.QualityView> close(@PathVariable Long id, @Valid @RequestBody DeliveryQualityService.CloseQualityRequest request) { return ApiResponse.ok(service.close(id, request)); }
}
