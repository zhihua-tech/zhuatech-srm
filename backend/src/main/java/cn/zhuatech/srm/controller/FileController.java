/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.controller;

import cn.zhuatech.srm.common.ApiResponse;
import cn.zhuatech.srm.service.FileStorageService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController @RequestMapping("/api/files")
public class FileController {
    private final FileStorageService storage;
    public FileController(FileStorageService storage) { this.storage = storage; }
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@permissionService.has('FILE')")
    public ApiResponse<Map<String, Object>> upload(@RequestPart("file") MultipartFile file,
                                                   @RequestParam String businessType,
                                                   @RequestParam(required = false) String businessId) {
        var asset = storage.store(file, businessType, businessId);
        return ApiResponse.ok(Map.of("fileKey", asset.getFileKey(), "name", asset.getOriginalName(), "size", asset.getSizeBytes()));
    }
    @GetMapping("/{fileKey}")
    @PreAuthorize("@permissionService.has('FILE')")
    public ResponseEntity<org.springframework.core.io.Resource> download(@PathVariable String fileKey) {
        var download = storage.load(fileKey);
        String disposition = "attachment; filename*=UTF-8''" + java.net.URLEncoder.encode(download.asset().getOriginalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, disposition)
            .contentType(MediaType.parseMediaType(download.asset().getContentType())).body(download.resource());
    }
}
