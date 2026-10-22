/* Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/ */
package cn.zhuatech.srm.service;

import cn.zhuatech.srm.common.BusinessException;
import cn.zhuatech.srm.model.*;
import cn.zhuatech.srm.repository.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** RFQ/RFP、定向邀请、报价版本、澄清、比价和定标。 */
@Service
@Transactional(readOnly = true)
public class SourcingService {
    private final SourcingEventRepository events;
    private final SourcingItemRepository items;
    private final SourcingInvitationRepository invitations;
    private final SupplierQuoteRepository quotes;
    private final SourcingClarificationRepository clarifications;
    private final SupplierRepository suppliers;
    private final UserRepository users;
    private final InboxItemRepository inbox;
    private final CurrentUserService current;
    private final AuditService audit;

    public SourcingService(SourcingEventRepository events, SourcingItemRepository items,
                           SourcingInvitationRepository invitations, SupplierQuoteRepository quotes,
                           SourcingClarificationRepository clarifications, SupplierRepository suppliers,
                           UserRepository users, InboxItemRepository inbox, CurrentUserService current, AuditService audit) {
        this.events = events; this.items = items; this.invitations = invitations; this.quotes = quotes;
        this.clarifications = clarifications; this.suppliers = suppliers; this.users = users;
        this.inbox = inbox; this.current = current; this.audit = audit;
    }

    public List<EventView> adminList() {
        return events.findByOrganizationCodeAndDeletedFalse(current.organizationCode()).stream().map(this::view).toList();
    }
    public List<EventView> portalList() {
        Long supplierId = current.requireSupplier().getId();
        return invitations.findBySupplierIdAndDeletedFalse(supplierId).stream()
            .map(SourcingInvitation::getEvent)
            .filter(event -> !event.isDeleted() && (event.getStatus() == SourcingEvent.Status.OPEN || event.getStatus() == SourcingEvent.Status.EVALUATING || event.getStatus() == SourcingEvent.Status.AWARDED))
            .map(this::view).toList();
    }
    public EventDetail detail(Long eventId) {
        SourcingEvent event = scopedEvent(eventId);
        return new EventDetail(view(event), items.findByEventIdAndDeletedFalseOrderByLineNo(eventId).stream().map(ItemView::from).toList());
    }

    @Transactional
    public EventDetail create(CreateEventRequest request) {
        if (!request.deadline().isAfter(LocalDateTime.now())) throw new BusinessException("报价截止时间必须晚于当前时间");
        if (events.findByOrganizationCodeAndDeletedFalse(current.organizationCode()).stream().anyMatch(e -> e.getEventNo().equals(request.eventNo()))) {
            throw new BusinessException("询价编号已存在");
        }
        SourcingEvent event = events.save(new SourcingEvent(current.organizationCode(), request.eventNo(), request.type(),
            request.title(), request.categoryName(), current.get().getFullName(), request.requirementDescription(),
            request.deadline(), request.budget()));
        int lineNo = 0;
        for (ItemRequest value : request.items()) {
            items.save(new SourcingItem(event, ++lineNo, value.materialCode(), value.materialName(), value.specification(),
                value.quantity(), value.unit(), value.requiredDate()));
        }
        Set<Long> uniqueSupplierIds = new LinkedHashSet<>(request.supplierIds());
        for (Long supplierId : uniqueSupplierIds) {
            Supplier supplier = suppliers.findByIdAndOrganizationCodeAndDeletedFalse(supplierId, current.organizationCode())
                .orElseThrow(() -> new BusinessException("受邀供应商不存在：" + supplierId));
            invitations.save(new SourcingInvitation(event, supplier));
        }
        if (request.publish()) {
            event.publish(uniqueSupplierIds.size());
            notifyInvitedSuppliers(event, uniqueSupplierIds);
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", request.publish() ? "CREATE_AND_PUBLISH" : "CREATE",
            "SOURCING_EVENT", event.getId(), null, Map.of("eventNo", event.getEventNo(), "supplierCount", uniqueSupplierIds.size()));
        return detail(event.getId());
    }

    @Transactional
    public EventView publish(Long eventId) {
        SourcingEvent event = adminEvent(eventId);
        List<SourcingInvitation> values = invitations.findByEventIdAndDeletedFalse(eventId);
        event.publish(values.size());
        notifyInvitedSuppliers(event, values.stream().map(v -> v.getSupplier().getId()).collect(java.util.stream.Collectors.toSet()));
        audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", "PUBLISH", "SOURCING_EVENT", eventId, null, event.getStatus());
        return view(event);
    }

    @Transactional
    public QuoteView submitQuote(Long eventId, QuoteRequest request) {
        Supplier supplier = current.requireSupplier();
        SourcingEvent event = events.findById(eventId).orElseThrow(() -> new BusinessException("询价项目不存在"));
        requireInvitation(event, supplier);
        if (event.getStatus() != SourcingEvent.Status.OPEN || !event.getDeadline().isAfter(LocalDateTime.now())) throw new BusinessException("该询价已停止接收报价");
        int version = quotes.findFirstByEventIdAndSupplierIdAndDeletedFalseOrderByQuoteVersionDesc(eventId, supplier.getId())
            .map(value -> value.getQuoteVersion() + 1).orElse(1);
        SupplierQuote quote = quotes.save(new SupplierQuote(event, supplier, version, request.amount(), request.taxRate(),
            request.leadDays(), request.validUntil(), request.remark(), request.attachmentKey()));
        if (version == 1) event.markResponded();
        audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", "QUOTE_SUBMIT", "SUPPLIER_QUOTE",
            quote.getId(), null, Map.of("eventId", eventId, "version", version, "amount", request.amount()));
        return QuoteView.from(quote);
    }

    public List<QuoteView> compare(Long eventId) {
        adminEvent(eventId);
        Map<Long, SupplierQuote> latest = new HashMap<>();
        for (SupplierQuote quote : quotes.findByEventIdAndDeletedFalseOrderByAmountAsc(eventId)) {
            latest.merge(quote.getSupplier().getId(), quote,
                (left, right) -> left.getQuoteVersion() >= right.getQuoteVersion() ? left : right);
        }
        return latest.values().stream().sorted(Comparator.comparing(SupplierQuote::getAmount)).map(QuoteView::from).toList();
    }

    @Transactional
    public QuoteView evaluate(Long eventId, Long quoteId, EvaluationRequest request) {
        SourcingEvent event = adminEvent(eventId);
        SupplierQuote quote = quotes.findById(quoteId).orElseThrow(() -> new BusinessException("报价不存在"));
        if (!quote.getEvent().getId().equals(eventId)) throw new BusinessException("报价不属于该询价");
        BigDecimal score = request.priceScore().multiply(new BigDecimal("0.40"))
            .add(request.deliveryScore().multiply(new BigDecimal("0.25")))
            .add(request.qualityScore().multiply(new BigDecimal("0.20")))
            .add(request.serviceScore().multiply(new BigDecimal("0.15"))).setScale(2, java.math.RoundingMode.HALF_UP);
        quote.score(score);
        if (event.getStatus() == SourcingEvent.Status.OPEN) event.startEvaluation();
        audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", "QUOTE_EVALUATE", "SUPPLIER_QUOTE",
            quoteId, null, Map.of("score", score, "comment", request.comment()));
        return QuoteView.from(quote);
    }

    @Transactional
    public ClarificationView ask(Long eventId, ClarificationRequest request) {
        Supplier supplier = current.requireSupplier();
        SourcingEvent event = events.findById(eventId).orElseThrow(() -> new BusinessException("询价项目不存在"));
        requireInvitation(event, supplier);
        return ClarificationView.from(clarifications.save(new SourcingClarification(event, supplier, request.content(), current.get().getUsername())));
    }

    @Transactional
    public ClarificationView answer(Long clarificationId, ClarificationRequest request) {
        SourcingClarification value = clarifications.findById(clarificationId).orElseThrow(() -> new BusinessException("澄清记录不存在"));
        if (!value.getEvent().getOrganizationCode().equals(current.organizationCode())) throw new BusinessException("无权访问该澄清记录");
        value.answer(request.content(), current.get().getUsername());
        return ClarificationView.from(value);
    }

    public List<ClarificationView> portalClarifications(Long eventId) {
        Supplier supplier = current.requireSupplier();
        requireInvitation(events.findById(eventId).orElseThrow(() -> new BusinessException("询价项目不存在")), supplier);
        return clarifications.findByEventIdAndSupplierIdAndDeletedFalseOrderByCreatedAt(eventId, supplier.getId()).stream().map(ClarificationView::from).toList();
    }

    @Transactional
    public EventView requestAward(Long eventId, AwardRequest request) {
        SourcingEvent event = adminEvent(eventId);
        SupplierQuote quote = quotes.findById(request.quoteId()).orElseThrow(() -> new BusinessException("报价不存在"));
        if (!quote.getEvent().getId().equals(eventId)) throw new BusinessException("报价不属于该询价");
        if (event.getStatus() == SourcingEvent.Status.OPEN) event.startEvaluation();
        if (quote.getEvaluationScore() == null) throw new BusinessException("请先完成报价综合评审");
        event.requestAwardApproval(quote.getId(), request.reason());
        audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", "AWARD_REQUEST", "SOURCING_EVENT", eventId,
            null, Map.of("supplierId", quote.getSupplier().getId(), "quoteId", quote.getId(), "reason", request.reason()));
        return view(event);
    }

    @Transactional
    public EventView approveAward(Long eventId, AwardApprovalRequest request) {
        SourcingEvent event = adminEvent(eventId);
        if (!request.approved()) {
            event.rejectAwardApproval();
            audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", "AWARD_REJECT", "SOURCING_EVENT", eventId,
                null, Map.of("comment", request.comment()));
            return view(event);
        }
        SupplierQuote quote = quotes.findById(event.getProposedQuoteId()).orElseThrow(() -> new BusinessException("待审批报价不存在"));
        event.award(quote.getSupplier());
        quote.markAwarded();
        for (SourcingInvitation invitation : invitations.findByEventIdAndDeletedFalse(eventId)) {
            String title = invitation.getSupplier().getId().equals(quote.getSupplier().getId()) ? "询价已定标：贵司中选" : "询价定标结果已发布";
            for (UserAccount user : users.findBySupplierIdAndEnabledTrue(invitation.getSupplier().getId())) {
                inbox.save(new InboxItem(event.getOrganizationCode(), user.getUsername(), invitation.getSupplier(), InboxItem.Kind.MESSAGE,
                    title, event.getEventNo() + " " + event.getTitle(), "SOURCING", String.valueOf(eventId), "/portal/rfqs/" + eventId, null));
            }
        }
        audit.record(current.organizationCode(), current.get().getUsername(), "SOURCING", "AWARD_APPROVE", "SOURCING_EVENT", eventId,
            null, Map.of("supplierId", quote.getSupplier().getId(), "quoteId", quote.getId(), "comment", request.comment()));
        return view(event);
    }

    private void notifyInvitedSuppliers(SourcingEvent event, Set<Long> supplierIds) {
        for (Long supplierId : supplierIds) {
            Supplier supplier = suppliers.findById(supplierId).orElseThrow();
            for (UserAccount user : users.findBySupplierIdAndEnabledTrue(supplierId)) {
                inbox.save(new InboxItem(event.getOrganizationCode(), user.getUsername(), supplier, InboxItem.Kind.TASK,
                    "待报价：" + event.getTitle(), "报价截止 " + event.getDeadline(), "SOURCING", String.valueOf(event.getId()),
                    "/portal/rfqs/" + event.getId(), event.getDeadline()));
            }
        }
    }
    private void requireInvitation(SourcingEvent event, Supplier supplier) {
        if (!event.getOrganizationCode().equals(supplier.getOrganizationCode())
            || !invitations.existsByEventIdAndSupplierIdAndDeletedFalse(event.getId(), supplier.getId())) {
            throw new BusinessException("无权访问该询价项目");
        }
    }
    private SourcingEvent adminEvent(Long id) {
        return events.findByIdAndOrganizationCodeAndDeletedFalse(id, current.organizationCode())
            .orElseThrow(() -> new BusinessException("询价项目不存在"));
    }
    private SourcingEvent scopedEvent(Long id) {
        if (current.isSupplier()) {
            SourcingEvent event = events.findById(id).orElseThrow(() -> new BusinessException("询价项目不存在"));
            requireInvitation(event, current.requireSupplier()); return event;
        }
        return adminEvent(id);
    }
    private EventView view(SourcingEvent event) {
        return new EventView(event.getId(), event.getEventNo(), event.getType(), event.getTitle(), event.getCategoryName(),
            event.getOwnerName(), event.getDeadline(), event.getBudget(), event.getInvitedCount(), event.getRespondedCount(),
            event.getStatus(), event.getAwardedSupplier() == null ? null : event.getAwardedSupplier().getSupplierName());
    }

    public record ItemRequest(@NotBlank String materialCode, @NotBlank String materialName, String specification,
                              @NotNull @DecimalMin("0.0001") BigDecimal quantity, @NotBlank String unit, @NotNull LocalDate requiredDate) {}
    public record CreateEventRequest(@NotBlank String eventNo, @NotNull SourcingEvent.Type type, @NotBlank String title,
        @NotBlank String categoryName, String requirementDescription, @Future LocalDateTime deadline,
        @NotNull @DecimalMin("0") BigDecimal budget, @NotEmpty List<@Valid ItemRequest> items,
        @NotEmpty List<Long> supplierIds, boolean publish) {}
    public record QuoteRequest(@NotNull @DecimalMin("0.01") BigDecimal amount, @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal taxRate,
        @Min(1) int leadDays, @Future LocalDate validUntil, @Size(max = 500) String remark, String attachmentKey) {}
    public record ClarificationRequest(@NotBlank @Size(max = 1000) String content) {}
    public record AwardRequest(@NotNull Long quoteId, @NotBlank @Size(max = 500) String reason) {}
    public record AwardApprovalRequest(boolean approved, @NotBlank @Size(max = 500) String comment) {}
    public record EvaluationRequest(@NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal priceScore,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal deliveryScore,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal qualityScore,
        @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal serviceScore,
        @NotBlank @Size(max = 500) String comment) {}
    public record EventView(Long id, String eventNo, SourcingEvent.Type type, String title, String category,
        String owner, LocalDateTime deadline, BigDecimal budget, int invited, int responded,
        SourcingEvent.Status status, String awardedSupplier) {}
    public record ItemView(Long id, int lineNo, String materialCode, String materialName, String specification,
        BigDecimal quantity, String unit, LocalDate requiredDate) {
        static ItemView from(SourcingItem value) { return new ItemView(value.getId(), value.getLineNo(), value.getMaterialCode(), value.getMaterialName(), value.getSpecification(), value.getQuantity(), value.getUnit(), value.getRequiredDate()); }
    }
    public record EventDetail(EventView event, List<ItemView> items) {}
    public record QuoteView(Long id, Long eventId, Long supplierId, String supplierName, int version, BigDecimal amount,
        BigDecimal taxRate, int leadDays, LocalDate validUntil, String remark, BigDecimal evaluationScore, String status) {
        static QuoteView from(SupplierQuote value) { return new QuoteView(value.getId(), value.getEvent().getId(), value.getSupplier().getId(), value.getSupplier().getSupplierName(), value.getQuoteVersion(), value.getAmount(), value.getTaxRate(), value.getLeadDays(), value.getValidUntil(), value.getRemark(), value.getEvaluationScore(), value.getStatus().name()); }
    }
    public record ClarificationView(Long id, Long eventId, Long supplierId, String question, String answer,
        String askedBy, String answeredBy, LocalDateTime answeredAt) {
        static ClarificationView from(SourcingClarification value) { return new ClarificationView(value.getId(), value.getEvent().getId(), value.getSupplier().getId(), value.getQuestion(), value.getAnswer(), value.getAskedBy(), value.getAnsweredBy(), value.getAnsweredAt()); }
    }
}
