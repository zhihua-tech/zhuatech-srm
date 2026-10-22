/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;

/** 供应商联系人。 */
@Entity
@Table(name = "srm_supplier_contact")
public class SupplierContact extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false, length = 50) private String name;
    @Column(length = 60) private String positionName;
    @Column(length = 30) private String phone;
    @Column(length = 120) private String email;
    @Column(nullable = false) private boolean primaryContact;
    protected SupplierContact() {}
    public SupplierContact(Supplier supplier, String name, String positionName, String phone, String email, boolean primaryContact) {
        this.supplier = supplier; this.name = name; this.positionName = positionName;
        this.phone = phone; this.email = email; this.primaryContact = primaryContact;
    }
    public Supplier getSupplier() { return supplier; }
    public String getName() { return name; }
    public String getPositionName() { return positionName; }
    public String getPhone() { return phone; }
    public String getEmail() { return email; }
    public boolean isPrimaryContact() { return primaryContact; }
}
