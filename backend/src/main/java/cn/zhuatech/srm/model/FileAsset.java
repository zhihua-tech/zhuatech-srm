/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;

/** 受访问控制的业务附件元数据。 */
@Entity @Table(name = "srm_file_asset")
public class FileAsset extends BaseEntity {
    @Column(nullable = false, unique = true, length = 64) private String fileKey;
    @Column(nullable = false, length = 32) private String organizationCode;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 255) private String originalName;
    @Column(nullable = false, length = 120) private String contentType;
    @Column(nullable = false) private long sizeBytes;
    @Column(nullable = false, length = 64) private String sha256;
    @Column(nullable = false, length = 500) private String storagePath;
    @Column(nullable = false, length = 80) private String businessType;
    @Column(length = 80) private String businessId;
    @Column(nullable = false, length = 64) private String uploadedBy;
    protected FileAsset() {}
    public FileAsset(String fileKey, String organizationCode, Supplier supplier, String originalName,
                     String contentType, long sizeBytes, String sha256, String storagePath,
                     String businessType, String businessId, String uploadedBy) {
        this.fileKey = fileKey; this.organizationCode = organizationCode; this.supplier = supplier;
        this.originalName = originalName; this.contentType = contentType; this.sizeBytes = sizeBytes;
        this.sha256 = sha256; this.storagePath = storagePath; this.businessType = businessType;
        this.businessId = businessId; this.uploadedBy = uploadedBy;
    }
    public String getFileKey() { return fileKey; }
    public String getOrganizationCode() { return organizationCode; }
    public Supplier getSupplier() { return supplier; }
    public String getOriginalName() { return originalName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getStoragePath() { return storagePath; }
    public String getBusinessType() { return businessType; }
    public String getBusinessId() { return businessId; }
    public String getUploadedBy() { return uploadedBy; }
}
