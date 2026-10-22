/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.FileAsset;
import cn.zhuatech.srm.model.Supplier;
import cn.zhuatech.srm.model.UserAccount;
import cn.zhuatech.srm.repository.FileAssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** 附件格式、大小、路径和访问权限控制。 */
@Service
public class FileStorageService {
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "png", "jpg", "jpeg", "xlsx", "docx", "zip");
    private final Path root;
    private final long maxSize;
    private final FileAssetRepository files;
    private final CurrentUserService current;
    private final AuditService audit;
    public FileStorageService(@Value("${app.file-storage.root}") String root,
                              @Value("${app.file-storage.max-size:20971520}") long maxSize,
                              FileAssetRepository files, CurrentUserService current, AuditService audit) {
        this.root = Path.of(root).toAbsolutePath().normalize(); this.maxSize = maxSize;
        this.files = files; this.current = current; this.audit = audit;
    }

    @Transactional
    public FileAsset store(MultipartFile upload, String businessType, String businessId) {
        if (upload == null || upload.isEmpty()) throw new BusinessException("请选择文件");
        if (upload.getSize() > maxSize) throw new BusinessException("文件超过大小限制");
        if (businessType == null || businessType.isBlank() || businessType.length() > 80) throw new BusinessException("业务类型不合法");
        if (businessId != null && businessId.length() > 80) throw new BusinessException("业务标识过长");
        String originalName = Path.of(upload.getOriginalFilename() == null ? "file" : upload.getOriginalFilename()).getFileName().toString();
        String extension = extension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) throw new BusinessException("不支持的文件格式");
        UserAccount user = current.get();
        Supplier supplier = user.getRole() == UserAccount.Role.SUPPLIER ? current.requireSupplier() : null;
        String fileKey = UUID.randomUUID().toString().replace("-", "");
        LocalDate today = LocalDate.now();
        Path directory = root.resolve(user.getOrganizationCode()).resolve(String.valueOf(today.getYear()))
            .resolve(String.format("%02d", today.getMonthValue())).normalize();
        if (!directory.startsWith(root)) throw new BusinessException("文件路径不合法");
        Path target = directory.resolve(fileKey + "." + extension).normalize();
        try {
            Files.createDirectories(directory);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] signature = new byte[8]; int signatureLength = 0;
            try (InputStream input = upload.getInputStream(); var output = Files.newOutputStream(target)) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) >= 0) {
                    if (signatureLength < signature.length) {
                        int copy = Math.min(count, signature.length - signatureLength);
                        System.arraycopy(buffer, 0, signature, signatureLength, copy); signatureLength += copy;
                    }
                    digest.update(buffer, 0, count); output.write(buffer, 0, count);
                }
            }
            if (!matchesSignature(extension, signature, signatureLength)) {
                Files.deleteIfExists(target); throw new BusinessException("文件内容与扩展名不匹配");
            }
            FileAsset asset = files.save(new FileAsset(fileKey, user.getOrganizationCode(), supplier, originalName,
                contentType(extension), upload.getSize(),
                HexFormat.of().formatHex(digest.digest()), target.toString(), businessType, businessId, user.getUsername()));
            audit.record(user.getOrganizationCode(), user.getUsername(), "FILE", "UPLOAD", "FILE_ASSET", fileKey, null,
                java.util.Map.of("name", originalName, "businessType", businessType, "businessId", businessId == null ? "" : businessId));
            return asset;
        } catch (IOException exception) {
            throw new BusinessException("文件保存失败");
        } catch (java.security.NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    public Download load(String fileKey) {
        FileAsset asset = files.findByFileKeyAndDeletedFalse(fileKey).orElseThrow(() -> new BusinessException("文件不存在"));
        UserAccount user = current.get();
        if (!asset.getOrganizationCode().equals(user.getOrganizationCode())) throw new BusinessException("无权访问该文件");
        if (user.getRole() == UserAccount.Role.SUPPLIER && (asset.getSupplier() == null || !asset.getSupplier().getId().equals(current.requireSupplier().getId()))) {
            throw new BusinessException("无权访问该文件");
        }
        try {
            Resource resource = new UrlResource(Path.of(asset.getStoragePath()).toUri());
            if (!resource.exists() || !resource.isReadable()) throw new BusinessException("文件不可读取");
            audit.record(user.getOrganizationCode(), user.getUsername(), "FILE", "DOWNLOAD", "FILE_ASSET", fileKey, null, null);
            return new Download(asset, resource);
        } catch (java.net.MalformedURLException exception) {
            throw new BusinessException("文件路径无效");
        }
    }

    private String extension(String name) {
        int index = name.lastIndexOf('.');
        return index < 0 ? "" : name.substring(index + 1).toLowerCase(Locale.ROOT);
    }
    private boolean matchesSignature(String extension, byte[] value, int length) {
        if (length < 2) return false;
        if (Set.of("xlsx", "docx", "zip").contains(extension)) return value[0] == 'P' && value[1] == 'K';
        if (extension.equals("pdf")) return length >= 4 && value[0] == '%' && value[1] == 'P' && value[2] == 'D' && value[3] == 'F';
        if (extension.equals("png")) return length >= 8 && (value[0] & 0xff) == 0x89 && value[1] == 'P' && value[2] == 'N' && value[3] == 'G';
        return Set.of("jpg", "jpeg").contains(extension) && (value[0] & 0xff) == 0xff && (value[1] & 0xff) == 0xd8;
    }
    private String contentType(String extension) {
        return switch (extension) {
            case "pdf" -> "application/pdf"; case "png" -> "image/png"; case "jpg", "jpeg" -> "image/jpeg";
            case "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/zip";
        };
    }
    public record Download(FileAsset asset, Resource resource) {}
}
