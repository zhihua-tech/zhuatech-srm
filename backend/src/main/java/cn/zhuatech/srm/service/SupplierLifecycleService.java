/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 供应商邀请、资料、证照、准入、状态和变更留痕。 */
@Service
@Transactional(readOnly = true)
public class SupplierLifecycleService {
    private final SupplierRepository suppliers;
    private final SupplierInvitationRepository invitations;
    private final SupplierContactRepository contacts;
    private final SupplierCertificateRepository certificates;
    private final SupplierOnboardingRepository onboarding;
    private final SupplierChangeRecordRepository changes;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUserService current;
    private final AuditService audit;
    private final ObjectMapper objectMapper;

    public SupplierLifecycleService(SupplierRepository suppliers, SupplierInvitationRepository invitations,
                                    SupplierContactRepository contacts, SupplierCertificateRepository certificates,
                                    SupplierOnboardingRepository onboarding, SupplierChangeRecordRepository changes,
                                    UserRepository users, PasswordEncoder encoder, CurrentUserService current,
                                    AuditService audit, ObjectMapper objectMapper) {
        this.suppliers = suppliers; this.invitations = invitations; this.contacts = contacts;
        this.certificates = certificates; this.onboarding = onboarding; this.changes = changes;
        this.users = users; this.encoder = encoder; this.current = current; this.audit = audit;
        this.objectMapper = objectMapper;
    }

    public Page<SupplierView> list(Pageable pageable) {
        return suppliers.findByOrganizationCodeAndDeletedFalse(current.organizationCode(), pageable).map(this::view);
    }

    public SupplierDetail detail(Long id) {
        Supplier supplier = scopedSupplier(id);
        return new SupplierDetail(view(supplier), contacts.findBySupplierIdAndDeletedFalse(id).stream().map(ContactView::from).toList(),
            certificates.findBySupplierIdAndDeletedFalse(id).stream().map(CertificateView::from).toList(),
            changes.findBySupplierIdAndDeletedFalseOrderByCreatedAtDesc(id).stream().map(ChangeView::from).toList(),
            onboarding.findFirstBySupplierIdAndDeletedFalseOrderByCreatedAtDesc(id).map(OnboardingView::from).orElse(null));
    }

    public SupplierDetail self() { return detail(current.requireSupplier().getId()); }

    /** 仅供当前供应商维护自身资料，银行原值不进入采购端通用详情响应。 */
    public SelfProfileDetail selfProfile() {
        Supplier supplier = current.requireSupplier();
        SupplierDetail detail = detail(supplier.getId());
        return new SelfProfileDetail(detail,
            new EditableProfile(supplier.getSupplierName(), supplier.getRegistrationNo(), supplier.getCategoryName(),
                supplier.getLegalRepresentative(), supplier.getRegisteredAddress(), supplier.getContactName(),
                supplier.getContactPhone(), supplier.getContactEmail(), supplier.getBankAccountName(),
                supplier.getBankName(), supplier.getBankAccountNo()));
    }

    @Transactional
    public InvitationView invite(InviteRequest request) {
        UserAccount actor = current.get();
        String token = UUID.randomUUID().toString().replace("-", "");
        SupplierInvitation invitation = invitations.save(new SupplierInvitation(actor.getOrganizationCode(), token,
            request.supplierName(), request.email(), request.contactName(), LocalDateTime.now().plusDays(request.validDays())));
        invitation.markSent();
        audit.record(actor.getOrganizationCode(), actor.getUsername(), "SUPPLIER", "INVITE", "SUPPLIER_INVITATION",
            invitation.getId(), null, Map.of("supplierName", request.supplierName(), "email", request.email()));
        return InvitationView.from(invitation);
    }

    @Transactional
    public SupplierView register(RegisterRequest request) {
        SupplierInvitation invitation = invitations.findByTokenAndDeletedFalse(request.invitationToken())
            .orElseThrow(() -> new BusinessException("邀请不存在或已失效"));
        if (users.findByUsername(request.username()).isPresent()) throw new BusinessException("用户名已存在");
        ensureNoDuplicate(invitation.getOrganizationCode(), request.registrationNo(), request.supplierName());
        try { invitation.accept(); } catch (IllegalStateException exception) { throw new BusinessException(exception.getMessage()); }
        Supplier supplier = suppliers.save(new Supplier(invitation.getOrganizationCode(), request.supplierCode(),
            request.supplierName(), request.registrationNo(), request.supplyScope(), request.contactName(), request.contactPhone()));
        supplier.updateProfile(request.supplierName(), request.registrationNo(), request.supplyScope(), request.legalRepresentative(),
            request.registeredAddress(), request.contactName(), request.contactPhone(), request.contactEmail(),
            request.bankAccountName(), request.bankName(), request.bankAccountNo());
        users.save(new UserAccount(request.username(), encoder.encode(request.password()), request.contactName(),
            UserAccount.Role.SUPPLIER, invitation.getOrganizationCode(), supplier));
        audit.record(invitation.getOrganizationCode(), request.username(), "SUPPLIER", "REGISTER", "SUPPLIER",
            supplier.getId(), null, Map.of("supplierCode", supplier.getSupplierCode(), "supplierName", supplier.getSupplierName()));
        return view(supplier);
    }

    @Transactional
    public SupplierView updateProfile(Long id, ProfileRequest request, String reason) {
        Supplier supplier = scopedSupplier(id);
        String before = supplier.getSupplierName() + "|" + supplier.getRegistrationNo() + "|" + supplier.getCategoryName();
        ensureNoDuplicateForUpdate(supplier, request.registrationNo(), request.supplierName());
        supplier.updateProfile(request.supplierName(), request.registrationNo(), request.supplyScope(), request.legalRepresentative(),
            request.registeredAddress(), request.contactName(), request.contactPhone(), request.contactEmail(),
            request.bankAccountName(), request.bankName(), request.bankAccountNo());
        String after = supplier.getSupplierName() + "|" + supplier.getRegistrationNo() + "|" + supplier.getCategoryName();
        changes.save(new SupplierChangeRecord(supplier, current.get().getUsername(), "PROFILE", before, after, reason));
        audit.record(supplier.getOrganizationCode(), current.get().getUsername(), "SUPPLIER", "PROFILE_UPDATE", "SUPPLIER",
            supplier.getId(), before, after);
        return view(supplier);
    }

    @Transactional
    public ContactView addContact(Long supplierId, ContactRequest request) {
        Supplier supplier = scopedSupplier(supplierId);
        return ContactView.from(contacts.save(new SupplierContact(supplier, request.name(), request.positionName(),
            request.phone(), request.email(), request.primaryContact())));
    }

    @Transactional
    public CertificateView addCertificate(Long supplierId, CertificateRequest request) {
        if (!request.expiresOn().isAfter(request.validFrom())) throw new BusinessException("证照失效日期必须晚于生效日期");
        Supplier supplier = scopedSupplier(supplierId);
        SupplierCertificate certificate = certificates.save(new SupplierCertificate(supplier, request.certificateType(),
            request.certificateNo(), request.validFrom(), request.expiresOn(), request.fileKey()));
        return CertificateView.from(certificate);
    }

    @Transactional
    public OnboardingView saveQuestionnaire(String questionnaireJson) {
        validateQuestionnaire(questionnaireJson, false);
        Supplier supplier = current.requireSupplier();
        SupplierOnboardingApplication application = onboarding
            .findFirstBySupplierIdAndDeletedFalseOrderByCreatedAtDesc(supplier.getId())
            .filter(value -> value.getStatus() == SupplierOnboardingApplication.Status.DRAFT
                || value.getStatus() == SupplierOnboardingApplication.Status.RETURNED)
            .orElseGet(() -> new SupplierOnboardingApplication(supplier, questionnaireJson));
        application.updateQuestionnaire(questionnaireJson);
        return OnboardingView.from(onboarding.save(application));
    }

    @Transactional
    public OnboardingView submitOnboarding() {
        Supplier supplier = current.requireSupplier();
        SupplierOnboardingApplication application = onboarding.findFirstBySupplierIdAndDeletedFalseOrderByCreatedAtDesc(supplier.getId())
            .orElseThrow(() -> new BusinessException("请先填写准入问卷"));
        validateQuestionnaire(application.getQuestionnaireJson(), true);
        application.submit(); supplier.changeStatus(Supplier.Status.PENDING_APPROVAL, "准入资料已提交");
        audit.record(supplier.getOrganizationCode(), current.get().getUsername(), "SUPPLIER", "ONBOARDING_SUBMIT",
            "ONBOARDING", application.getId(), null, application.getStatus());
        return OnboardingView.from(application);
    }

    private void validateQuestionnaire(String questionnaireJson, boolean requireComplete) {
        if (questionnaireJson == null || questionnaireJson.isBlank() || questionnaireJson.length() > 20000) throw new BusinessException("准入问卷不能为空且不能超过 20000 字符");
        try {
            JsonNode root = objectMapper.readTree(questionnaireJson);
            if (!root.isObject()) throw new BusinessException("准入问卷必须是 JSON 对象");
            if (requireComplete) {
                List<String> required = List.of("capacity", "qualitySystem", "deliveryCapability", "complianceStatement", "serviceCapability");
                List<String> missing = required.stream().filter(name -> root.path(name).asText("").isBlank()).toList();
                if (!missing.isEmpty()) throw new BusinessException("准入问卷未填写完整：" + String.join(", ", missing));
            }
        } catch (JsonProcessingException exception) { throw new BusinessException("准入问卷 JSON 格式不正确"); }
    }

    @Transactional
    public OnboardingView approveOnboarding(Long applicationId, ApprovalRequest request) {
        SupplierOnboardingApplication application = onboarding.findById(applicationId)
            .orElseThrow(() -> new BusinessException("准入申请不存在"));
        if (!application.getSupplier().getOrganizationCode().equals(current.organizationCode())) throw new BusinessException("无权审批该申请");
        switch (request.action()) {
            case "FIRST_APPROVE" -> application.firstApprove(current.get().getUsername(), request.comment());
            case "FINAL_APPROVE" -> { application.finalApprove(current.get().getUsername(), request.comment()); application.getSupplier().changeStatus(Supplier.Status.ACTIVE, "准入审批通过"); application.getSupplier().updateQualificationProgress(100); }
            case "RETURN" -> { application.returnForRevision(current.get().getUsername(), request.comment()); application.getSupplier().changeStatus(Supplier.Status.ONBOARDING, "准入资料退回修改"); }
            default -> throw new BusinessException("不支持的审批动作");
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "SUPPLIER", request.action(), "ONBOARDING",
            applicationId, null, Map.of("status", application.getStatus(), "comment", request.comment()));
        return OnboardingView.from(application);
    }

    @Transactional
    public SupplierView changeStatus(Long supplierId, StatusRequest request) {
        Supplier supplier = scopedSupplier(supplierId);
        Supplier.Status before = supplier.getStatus();
        supplier.changeStatus(request.status(), request.reason());
        changes.save(new SupplierChangeRecord(supplier, current.get().getUsername(), "STATUS", before.name(), request.status().name(), request.reason()));
        audit.record(current.organizationCode(), current.get().getUsername(), "SUPPLIER", "STATUS_CHANGE", "SUPPLIER", supplierId,
            before, Map.of("status", request.status(), "reason", request.reason()));
        return view(supplier);
    }

    private Supplier scopedSupplier(Long id) {
        UserAccount user = current.get();
        if (user.getRole() == UserAccount.Role.SUPPLIER) {
            Supplier self = current.requireSupplier();
            if (!self.getId().equals(id)) throw new BusinessException("无权访问其他供应商数据");
            return self;
        }
        return suppliers.findByIdAndOrganizationCodeAndDeletedFalse(id, user.getOrganizationCode())
            .orElseThrow(() -> new BusinessException("供应商不存在"));
    }

    private void ensureNoDuplicate(String org, String registrationNo, String name) {
        if (suppliers.existsByOrganizationCodeAndRegistrationNoAndDeletedFalse(org, registrationNo)
            || suppliers.existsByOrganizationCodeAndSupplierNameIgnoreCaseAndDeletedFalse(org, name)) {
            throw new BusinessException("检测到重复供应商，请核对统一社会信用代码和企业名称");
        }
    }
    private void ensureNoDuplicateForUpdate(Supplier supplier, String registrationNo, String name) {
        if ((!registrationNo.equals(supplier.getRegistrationNo()) && suppliers.existsByOrganizationCodeAndRegistrationNoAndDeletedFalse(supplier.getOrganizationCode(), registrationNo))
            || (!name.equalsIgnoreCase(supplier.getSupplierName()) && suppliers.existsByOrganizationCodeAndSupplierNameIgnoreCaseAndDeletedFalse(supplier.getOrganizationCode(), name))) {
            throw new BusinessException("检测到重复供应商");
        }
    }

    private SupplierView view(Supplier supplier) {
        return new SupplierView(supplier.getId(), supplier.getSupplierCode(), supplier.getSupplierName(), supplier.getRegistrationNo(),
            supplier.getCategoryName(), supplier.getContactName(), supplier.getContactPhone(), supplier.getContactEmail(),
            supplier.getStatus(), supplier.getQualificationProgress(), supplier.getRiskLevel(), supplier.maskedBankAccountNo());
    }

    public record InviteRequest(@NotBlank String supplierName, @NotBlank @Email String email, @NotBlank String contactName,
                                @Min(1) @Max(30) int validDays) {}
    public record InvitationView(Long id, String supplierName, String email, String status, LocalDateTime expiresAt, String token) {
        static InvitationView from(SupplierInvitation value) { return new InvitationView(value.getId(), value.getSupplierName(), value.getEmail(), value.getStatus().name(), value.getExpiresAt(), value.getToken()); }
    }
    public record RegisterRequest(@NotBlank String invitationToken, @NotBlank String supplierCode,
        @NotBlank String supplierName, @NotBlank @Pattern(regexp = "^[0-9A-Z]{15,18}$", message = "统一社会信用代码格式不正确") String registrationNo,
        @NotBlank String supplyScope, @NotBlank String legalRepresentative, @NotBlank String registeredAddress,
        @NotBlank String contactName, @NotBlank String contactPhone, @NotBlank @Email String contactEmail,
        @NotBlank String bankAccountName, @NotBlank String bankName, @NotBlank String bankAccountNo,
        @NotBlank String username, @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{12,72}$", message = "密码至少 12 位并包含字母和数字") String password) {}
    public record ProfileRequest(@NotBlank String supplierName,
        @NotBlank @Pattern(regexp = "^[0-9A-Z]{15,18}$") String registrationNo, @NotBlank String supplyScope,
        @NotBlank String legalRepresentative, @NotBlank String registeredAddress, @NotBlank String contactName,
        @NotBlank String contactPhone, @NotBlank @Email String contactEmail, @NotBlank String bankAccountName,
        @NotBlank String bankName, @NotBlank String bankAccountNo) {}
    public record ContactRequest(@NotBlank String name, String positionName, String phone, @Email String email, boolean primaryContact) {}
    public record CertificateRequest(@NotBlank String certificateType, @NotBlank String certificateNo,
                                     @NotNull LocalDate validFrom, @NotNull LocalDate expiresOn, String fileKey) {}
    public record ApprovalRequest(@NotBlank String action, @NotBlank String comment) {}
    public record StatusRequest(@NotNull Supplier.Status status, @NotBlank String reason) {}
    public record SupplierView(Long id, String code, String name, String registrationNo, String supplyScope,
        String contactName, String contactPhone, String contactEmail, Supplier.Status status,
        int qualificationProgress, String riskLevel, String maskedBankAccountNo) {}
    public record ContactView(Long id, String name, String positionName, String phone, String email, boolean primaryContact) {
        static ContactView from(SupplierContact value) { return new ContactView(value.getId(), value.getName(), value.getPositionName(), value.getPhone(), value.getEmail(), value.isPrimaryContact()); }
    }
    public record CertificateView(Long id, String type, String no, LocalDate validFrom, LocalDate expiresOn, boolean verified, String fileKey) {
        static CertificateView from(SupplierCertificate value) { return new CertificateView(value.getId(), value.getCertificateType(), value.getCertificateNo(), value.getValidFrom(), value.getExpiresOn(), value.isVerified(), value.getFileKey()); }
    }
    public record ChangeView(Long id, String field, String beforeValue, String afterValue, String reason, String changedBy, LocalDateTime changedAt) {
        static ChangeView from(SupplierChangeRecord value) { return new ChangeView(value.getId(), value.getFieldName(), value.getBeforeValue(), value.getAfterValue(), value.getReason(), value.getChangedBy(), value.getCreatedAt()); }
    }
    public record SupplierDetail(SupplierView supplier, List<ContactView> contacts, List<CertificateView> certificates,
                                 List<ChangeView> changeHistory, OnboardingView onboarding) {}
    public record EditableProfile(String supplierName, String registrationNo, String supplyScope,
        String legalRepresentative, String registeredAddress, String contactName, String contactPhone,
        String contactEmail, String bankAccountName, String bankName, String bankAccountNo) {}
    public record SelfProfileDetail(SupplierDetail detail, EditableProfile profile) {}
    public record OnboardingView(Long id, Long supplierId, String status, String questionnaireJson, String comment) {
        static OnboardingView from(SupplierOnboardingApplication value) { return new OnboardingView(value.getId(), value.getSupplier().getId(), value.getStatus().name(), value.getQuestionnaireJson(), value.getApprovalComment()); }
    }
}
