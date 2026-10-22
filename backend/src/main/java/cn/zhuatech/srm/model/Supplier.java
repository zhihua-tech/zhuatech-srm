/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.math.BigDecimal;

/**
 * 供应商主数据。敏感银行信息只能通过受控接口访问，不在普通列表中返回。
 * 商业授权或定制开发请微信添加微信号 zhuatech 或 zhuatech2 进行咨询。
 */
@Entity
@Table(name = "srm_supplier", uniqueConstraints = {
    @UniqueConstraint(name = "uk_supplier_org_code", columnNames = {"organization_code", "supplier_code"}),
    @UniqueConstraint(name = "uk_supplier_org_registration", columnNames = {"organization_code", "registration_no"})
})
public class Supplier extends BaseEntity {
    public enum Status { ONBOARDING, PENDING_APPROVAL, ACTIVE, RECTIFYING, SUSPENDED, ELIMINATED, BLACKLISTED }

    @Column(nullable = false, length = 32)
    private String organizationCode = "DEFAULT";

    @Column(nullable = false, length = 24)
    private String supplierCode;

    @Column(nullable = false, length = 100)
    private String supplierName;

    @Column(length = 32)
    private String registrationNo;

    @Column(nullable = false, length = 200)
    private String categoryName;

    @Column(length = 80)
    private String legalRepresentative;

    @Column(length = 300)
    private String registeredAddress;

    @Column(length = 40)
    private String contactName;

    @Column(length = 30)
    private String contactPhone;

    @Column(length = 120)
    private String contactEmail;

    @Column(length = 120)
    private String bankAccountName;

    @Column(length = 120)
    private String bankName;

    @Column(length = 64)
    private String bankAccountNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private Status status;

    @Column(nullable = false)
    private int qualificationProgress;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal onTimeRate = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String riskLevel = "LOW";

    @Column(length = 500)
    private String statusReason;

    protected Supplier() {}

    public Supplier(String organizationCode, String code, String name, String registrationNo,
                    String category, String contact, String phone) {
        this.organizationCode = organizationCode;
        this.supplierCode = code;
        this.supplierName = name;
        this.registrationNo = registrationNo;
        this.categoryName = category;
        this.contactName = contact;
        this.contactPhone = phone;
        this.status = Status.ONBOARDING;
    }

    /** 兼容旧版扩展代码。 */
    public Supplier(String code, String name, String category, String contact, String phone,
                    Status status, int progress, BigDecimal rating, BigDecimal onTimeRate, String risk) {
        this("DEFAULT", code, name, null, category, contact, phone);
        this.status = status;
        this.qualificationProgress = progress;
        this.rating = rating;
        this.onTimeRate = onTimeRate;
        this.riskLevel = risk;
    }

    public void updateProfile(String name, String registrationNo, String category, String legalRepresentative,
                              String registeredAddress, String contactName, String contactPhone, String contactEmail,
                              String bankAccountName, String bankName, String bankAccountNo) {
        this.supplierName = name;
        this.registrationNo = registrationNo;
        this.categoryName = category;
        this.legalRepresentative = legalRepresentative;
        this.registeredAddress = registeredAddress;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.contactEmail = contactEmail;
        this.bankAccountName = bankAccountName;
        this.bankName = bankName;
        this.bankAccountNo = bankAccountNo;
    }

    public void changeStatus(Status target, String reason) {
        if (target == null) throw new IllegalArgumentException("供应商状态不能为空");
        this.status = target;
        this.statusReason = reason;
    }

    public void updateQualificationProgress(int progress) {
        this.qualificationProgress = Math.max(0, Math.min(100, progress));
    }

    public String maskedBankAccountNo() {
        if (bankAccountNo == null || bankAccountNo.length() < 8) return "";
        return bankAccountNo.substring(0, 4) + " **** **** " + bankAccountNo.substring(bankAccountNo.length() - 4);
    }

    public String getOrganizationCode() { return organizationCode; }
    public String getSupplierCode() { return supplierCode; }
    public String getSupplierName() { return supplierName; }
    public String getRegistrationNo() { return registrationNo; }
    public String getCategoryName() { return categoryName; }
    public String getLegalRepresentative() { return legalRepresentative; }
    public String getRegisteredAddress() { return registeredAddress; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public String getContactEmail() { return contactEmail; }
    public String getBankAccountName() { return bankAccountName; }
    public String getBankName() { return bankName; }
    public String getBankAccountNo() { return bankAccountNo; }
    public Status getStatus() { return status; }
    public int getQualificationProgress() { return qualificationProgress; }
    public BigDecimal getRating() { return rating; }
    public BigDecimal getOnTimeRate() { return onTimeRate; }
    public String getRiskLevel() { return riskLevel; }
    public String getStatusReason() { return statusReason; }
}
