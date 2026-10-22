/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 供应商周期绩效结果。固定权重：质量 35%、交付 35%、成本 15%、服务 15%。 */
@Entity @Table(name = "srm_supplier_performance")
public class SupplierPerformance extends BaseEntity {
    @Column(nullable = false, length = 32) private String organizationCode;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "supplier_id") private Supplier supplier;
    @Column(nullable = false) private LocalDate periodFrom;
    @Column(nullable = false) private LocalDate periodTo;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal qualityScore;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal deliveryScore;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal costScore;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal serviceScore;
    @Column(nullable = false, precision = 5, scale = 2) private BigDecimal totalScore;
    @Column(nullable = false, length = 24) private String grade;
    @Column(nullable = false) private boolean correctiveActionRequired;
    protected SupplierPerformance() {}
    public SupplierPerformance(String organizationCode, Supplier supplier, LocalDate periodFrom, LocalDate periodTo,
                               BigDecimal qualityScore, BigDecimal deliveryScore, BigDecimal costScore,
                               BigDecimal serviceScore) {
        this.organizationCode = organizationCode; this.supplier = supplier; this.periodFrom = periodFrom; this.periodTo = periodTo;
        this.qualityScore = qualityScore; this.deliveryScore = deliveryScore; this.costScore = costScore; this.serviceScore = serviceScore;
        totalScore = qualityScore.multiply(new BigDecimal("0.35")).add(deliveryScore.multiply(new BigDecimal("0.35")))
            .add(costScore.multiply(new BigDecimal("0.15"))).add(serviceScore.multiply(new BigDecimal("0.15")));
        grade = totalScore.compareTo(new BigDecimal("85")) >= 0 ? "A" : totalScore.compareTo(new BigDecimal("70")) >= 0 ? "B" : totalScore.compareTo(new BigDecimal("60")) >= 0 ? "C" : "D";
        correctiveActionRequired = totalScore.compareTo(new BigDecimal("70")) < 0;
    }
    public String getOrganizationCode() { return organizationCode; }
    public Supplier getSupplier() { return supplier; }
    public LocalDate getPeriodFrom() { return periodFrom; }
    public LocalDate getPeriodTo() { return periodTo; }
    public BigDecimal getQualityScore() { return qualityScore; }
    public BigDecimal getDeliveryScore() { return deliveryScore; }
    public BigDecimal getCostScore() { return costScore; }
    public BigDecimal getServiceScore() { return serviceScore; }
    public BigDecimal getTotalScore() { return totalScore; }
    public String getGrade() { return grade; }
    public boolean isCorrectiveActionRequired() { return correctiveActionRequired; }
}
