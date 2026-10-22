/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;
import jakarta.persistence.*;
/** 采购组织/租户主数据。 */
@Entity @Table(name = "srm_organization")
public class Organization extends BaseEntity {
    @Column(nullable = false, unique = true, length = 32) private String organizationCode;
    @Column(nullable = false, length = 120) private String organizationName;
    @Column(nullable = false) private boolean enabled = true;
    protected Organization() {}
    public Organization(String code, String name) { organizationCode = code; organizationName = name; }
    public void setEnabled(boolean value) { enabled = value; }
    public String getOrganizationCode() { return organizationCode; }
    public String getOrganizationName() { return organizationName; }
    public boolean isEnabled() { return enabled; }
}
