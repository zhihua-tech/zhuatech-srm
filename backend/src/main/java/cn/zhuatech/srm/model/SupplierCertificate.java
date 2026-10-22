/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.time.LocalDate;

/** 供应商资质证照及到期提醒信息。 */
@Entity
@Table(name = "srm_supplier_certificate", indexes = {
    @Index(name = "idx_certificate_supplier_expiry", columnList = "supplier_id,expires_on")
})
public class SupplierCertificate extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 40) private String certificateType;
    @Column(nullable = false, length = 80) private String certificateNo;
    @Column(nullable = false) private LocalDate validFrom;
    @Column(nullable = false) private LocalDate expiresOn;
    @Column(length = 64) private String fileKey;
    @Column(nullable = false) private boolean verified;
    protected SupplierCertificate() {}
    public SupplierCertificate(Supplier supplier, String certificateType, String certificateNo,
                               LocalDate validFrom, LocalDate expiresOn, String fileKey) {
        this.supplier = supplier; this.certificateType = certificateType; this.certificateNo = certificateNo;
        this.validFrom = validFrom; this.expiresOn = expiresOn; this.fileKey = fileKey;
    }
    public void verify() { verified = true; }
    public Supplier getSupplier() { return supplier; }
    public String getCertificateType() { return certificateType; }
    public String getCertificateNo() { return certificateNo; }
    public LocalDate getValidFrom() { return validFrom; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public String getFileKey() { return fileKey; }
    public boolean isVerified() { return verified; }
}
