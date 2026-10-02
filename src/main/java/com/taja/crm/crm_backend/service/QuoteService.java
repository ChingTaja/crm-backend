package com.taja.crm.crm_backend.service;

import com.taja.crm.crm_backend.dto.PageResponse;
import com.taja.crm.crm_backend.dto.quote.*;
import com.taja.crm.crm_backend.model.*;
import com.taja.crm.crm_backend.repo.*;
import java.math.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuoteService {
    private final QuoteRepository quotes;
    private final SalesOrderRepository orders;
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final OpportunityRepository opportunities;
    private final UserRepository users;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Taipei");
    private static final BigDecimal MAX_CENTS = new BigDecimal("9007199254740991");

    private LocalDate today() { return LocalDate.now(clock.withZone(BUSINESS_ZONE)); }
    private QuoteException error(HttpStatus status, String code, String detail) { return new QuoteException(status, code, detail); }
    private QuoteException conflict(String detail) { return error(HttpStatus.CONFLICT, "QUOTE_INVALID_STATE", detail); }
    private User actor(String actorId) {
        if (actorId == null) throw error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "請先登入。");
        return users.findById(actorId).orElseThrow(() -> error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "請重新登入。"));
    }
    private boolean manager(User user) {
        return user.getRole() != null && Set.of("ADMIN", "MANAGER").contains(user.getRole().getCode());
    }
    private void authorize(Quote quote, User actor) {
        if (!manager(actor) && !quote.getCreatedBy().equals(actor.getId()))
            throw error(HttpStatus.FORBIDDEN, "QUOTE_FORBIDDEN", "您沒有操作此報價單的權限。");
    }
    private Quote load(String id, User actor, boolean lock) {
        Quote quote = (lock ? quotes.findForUpdateById(id) : quotes.findById(id))
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "QUOTE_NOT_FOUND", "找不到報價單。"));
        authorize(quote, actor);
        return quote;
    }
    private QuoteVersion latest(Quote quote) { return quote.getVersions().getLast(); }
    private QuoteVersion version(Quote quote, String id) {
        QuoteVersion version = quote.getVersions().stream().filter(v -> v.getId().equals(id)).findFirst()
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "QUOTE_VERSION_NOT_FOUND", "此版本不屬於此報價單。"));
        if (latest(quote) != version) throw conflict("歷史版本為唯讀，請操作最新版本。");
        return version;
    }
    private void revision(QuoteVersion version, long expected) {
        if (version.getRevision() != expected)
            throw error(HttpStatus.CONFLICT, "QUOTE_REVISION_CONFLICT", "報價單已被其他使用者更新，請重新載入。");
    }
    private void unconverted(Quote quote) {
        if (quote.getOrderId() != null) throw conflict("報價单已轉為訂單，不可再修改。");
    }
    private void notExpired(QuoteVersion version) {
        if (version.getTerms().getValidUntil().isBefore(today()))
            throw error(HttpStatus.CONFLICT, "QUOTE_EXPIRED", "報價單已過期，請建立新版本。");
    }
    private QuoteResponse response(Quote quote) { return QuoteResponse.of(quote, today()); }

    public PageResponse<QuoteSummaryResponse> findAllQuotes(String actorId, Pageable pageable) {
        User actor = actor(actorId);
        Page<Quote> page = manager(actor) ? quotes.findAll(pageable) : quotes.findByCreatedBy(actorId, pageable);
        return PageResponse.fromPage(page.map(q -> QuoteSummaryResponse.of(q, latest(q), today())));
    }
    public QuoteResponse findByIdQuote(String actorId, String id) { return response(load(id, actor(actorId), false)); }

    @Transactional
    public QuoteResponse createQuotes(String actorId, @NotNull @Valid CreateQuoteRequest request) {
        User actor = actor(actorId);
        Quote quote = new Quote(); quote.setCreatedBy(actorId);
        long sequence = jdbc.queryForObject("select nextval('quote_number_seq')", Long.class);
        quote.setNumber("QT-" + today().format(DateTimeFormatter.ofPattern("yyyyMM")) + "-" + String.format(Locale.ROOT, "%04d", sequence));
        QuoteVersion version = new QuoteVersion(); version.setQuote(quote); version.setVersion(1);
        version.setCreatedAt(clock.instant()); version.setCreatedBy(actorId);
        apply(version, request, true);
        quote.getVersions().add(version);
        audit(quote, version, actor, "CREATED", "建立報價單。");
        quotes.saveAndFlush(quote);
        return response(quote);
    }

    @Transactional
    public QuoteResponse updateQuotes(String actorId, String id, String versionId, @NotNull @Valid UpdateQuoteRequest request) {
        User actor = actor(actorId); Quote quote = load(id, actor, true); unconverted(quote);
        QuoteVersion version = version(quote, versionId); revision(version, request.getExpectedRevision());
        if (version.getStatus() != QuoteStatus.Draft || version.getApproval() == ApprovalStatus.Pending || version.getApproval() == ApprovalStatus.Approved)
            throw conflict("此版本不可直接修改，請建立新版本。");
        apply(version, request, false);
        version.setRevision(version.getRevision() + 1);
        audit(quote, version, actor, "UPDATED", "更新報價內容與明細。");
        quotes.flush(); return response(quote);
    }

    @Transactional
    public void deleteQuotes(String actorId, String id) {
        User actor = actor(actorId); Quote quote = load(id, actor, true); unconverted(quote);
        if (quote.getVersions().stream().anyMatch(v -> v.getStatus() != QuoteStatus.Draft || v.getSentAt() != null || v.getApproval() == ApprovalStatus.Pending))
            throw conflict("僅能刪除從未送出、且沒有審批中版本的草稿報價單。");
        quotes.delete(quote); quotes.flush();
    }

    @Transactional
    public QuoteResponse newVersion(String actorId, String id, String versionId, @NotNull @Valid QuoteActionRequest request) {
        User actor = actor(actorId); Quote quote = load(id, actor, true); unconverted(quote);
        QuoteVersion source = version(quote, versionId); revision(source, request.expectedRevision());
        if (source.getStatus() == QuoteStatus.Accepted) throw conflict("已接受的報價不可建立新版本。");
        QuoteVersion next = new QuoteVersion(); next.setQuote(quote); next.setVersion(source.getVersion() + 1);
        next.setCreatedAt(clock.instant()); next.setCreatedBy(actorId); next.setTerms(copyTerms(source.getTerms()));
        for (QuoteLine line : source.getLines()) next.getLines().add(copyLine(line, false));
        next.setTotals(calculate(next.getLines()));
        boolean required = requiresApproval(next);
        next.setApproval(required ? ApprovalStatus.Required : ApprovalStatus.NotRequired);
        next.setRequiresReapproval(required && (source.getApproval() == ApprovalStatus.Approved || source.isRequiresReapproval()));
        source.setRevision(source.getRevision() + 1);
        quote.getVersions().add(next);
        audit(quote, next, actor, "NEW_VERSION", "由版本 " + source.getVersion() + " 建立新版本，保留產品快照。");
        quotes.saveAndFlush(quote); return response(quote);
    }

    @Transactional
    public QuoteResponse requestApproval(String actorId, String id, String versionId, @NotNull @Valid QuoteActionRequest request) {
        User actor = actor(actorId); Quote quote = load(id, actor, true); unconverted(quote);
        QuoteVersion version = version(quote, versionId); revision(version, request.expectedRevision());
        if (version.getStatus() != QuoteStatus.Draft || !requiresApproval(version)
                || (version.getApproval() != ApprovalStatus.Required && version.getApproval() != ApprovalStatus.Rejected))
            throw conflict("目前版本無法申請審批。");
        notExpired(version);
        version.setApproval(ApprovalStatus.Pending); clearReview(version);
        version.setRevision(version.getRevision() + 1);
        audit(quote, version, actor, "APPROVAL_REQUESTED", "送交主管審批。");
        quotes.flush(); return response(quote);
    }

    @Transactional
    public QuoteResponse review(String actorId, String id, String versionId, @NotNull @Valid ReviewQuoteRequest request) {
        User actor = actor(actorId);
        if (!manager(actor)) throw error(HttpStatus.FORBIDDEN, "QUOTE_REVIEW_FORBIDDEN", "只有主管可以審批報價單。");
        Quote quote = load(id, actor, true); unconverted(quote);
        QuoteVersion version = version(quote, versionId); revision(version, request.expectedRevision());
        if (version.getCreatedBy().equals(actorId)) throw error(HttpStatus.FORBIDDEN, "QUOTE_SELF_REVIEW", "不可審批自己建立的版本。");
        if (version.getStatus() != QuoteStatus.Draft || version.getApproval() != ApprovalStatus.Pending) throw conflict("此版本不在審批中。");
        boolean approved = "approved".equals(request.decision());
        reason(approved, request.reason()); notExpired(version);
        version.setApproval(approved ? ApprovalStatus.Approved : ApprovalStatus.Rejected);
        version.setApprovalBy(actorId); version.setApprovalAt(clock.instant()); version.setApprovalReason(request.reason());
        if (approved) version.setRequiresReapproval(false);
        version.setRevision(version.getRevision() + 1);
        audit(quote, version, actor, approved ? "APPROVED" : "APPROVAL_REJECTED", text(request.reason()));
        quotes.flush(); return response(quote);
    }

    @Transactional
    public QuoteResponse send(String actorId, String id, String versionId, @NotNull @Valid QuoteActionRequest request) {
        User actor = actor(actorId); Quote quote = load(id, actor, true); unconverted(quote);
        QuoteVersion version = version(quote, versionId); revision(version, request.expectedRevision());
        if (version.getStatus() != QuoteStatus.Draft || version.getApproval() == ApprovalStatus.Pending) throw conflict("只有草稿可送出。");
        notExpired(version);
        if (requiresApproval(version) && (version.getApproval() != ApprovalStatus.Approved || version.isRequiresReapproval()))
            throw conflict("報價單尚未通過必要的主管審批。");
        version.setStatus(QuoteStatus.Sent); version.setSentAt(clock.instant()); version.setRevision(version.getRevision() + 1);
        audit(quote, version, actor, "SENT", "報價已送出。"); quotes.flush(); return response(quote);
    }

    @Transactional
    public QuoteResponse decision(String actorId, String id, String versionId, @NotNull @Valid DecideQuoteRequest request) {
        User actor = actor(actorId); Quote quote = load(id, actor, true); unconverted(quote);
        QuoteVersion version = version(quote, versionId); revision(version, request.expectedRevision());
        if (version.getStatus() != QuoteStatus.Sent) throw conflict("只有已送出的報價可記錄客戶回覆。");
        boolean accepted = "accepted".equals(request.decision()); reason(accepted, request.reason());
        if (accepted) notExpired(version);
        version.setStatus(accepted ? QuoteStatus.Accepted : QuoteStatus.Rejected);
        version.setDecisionAt(clock.instant()); version.setDecisionBy(actorId); version.setDecisionReason(request.reason());
        version.setRevision(version.getRevision() + 1);
        audit(quote, version, actor, accepted ? "CUSTOMER_ACCEPTED" : "CUSTOMER_REJECTED", "代錄客戶回覆：" + text(request.reason()));
        quotes.flush(); return response(quote);
    }

    @Transactional
    public ConvertQuoteToOrderResponse convertToOrder(String actorId, String id, String versionId, @NotNull @Valid QuoteActionRequest request) {
        User actor = actor(actorId); Quote quote = load(id, actor, true);
        QuoteVersion version = version(quote, versionId);
        // 已轉單的同版本重試可使用原 revision，回傳同一 orderId。
        if (quote.getOrderId() != null) return new ConvertQuoteToOrderResponse(quote.getOrderId(), response(quote));
        revision(version, request.expectedRevision());
        if (version.getStatus() != QuoteStatus.Accepted) throw conflict("只有已接受的報價可以轉訂單。");
        SalesOrder order = new SalesOrder(); order.setQuote(quote); order.setQuoteVersionId(versionId);
        order.setCreatedAt(clock.instant()); order.setCreatedBy(actorId); order.setTerms(copyTerms(version.getTerms()));
        QuoteTotals totals = new QuoteTotals(); BeanUtils.copyProperties(version.getTotals(), totals); order.setTotals(totals);
        for (QuoteLine line : version.getLines()) order.getLines().add(copyLine(line, true));
        orders.saveAndFlush(order);
        quote.setOrderId(order.getId()); version.setRevision(version.getRevision() + 1);
        audit(quote, version, actor, "CONVERTED_TO_ORDER", "建立訂單 " + order.getId());
        quotes.flush(); return new ConvertQuoteToOrderResponse(order.getId(), response(quote));
    }

    private void apply(QuoteVersion version, CreateQuoteRequest request, boolean create) {
        QuoteTerms terms = new QuoteTerms(); terms.setName(request.getName().strip());
        if (terms.getName().isEmpty()) throw error(HttpStatus.BAD_REQUEST, "QUOTE_INVALID_NAME", "報價名稱不可空白。");
        terms.setCustomer(customers.findById(request.getCustomerId()).orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "QUOTE_INVALID_CUSTOMER", "所屬客戶不存在。")));
        if (request.getOpportunityId() != null) {
            Opportunity opportunity = opportunities.findById(request.getOpportunityId()).orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "QUOTE_INVALID_OPPORTUNITY", "商機不存在。"));
            if (!opportunity.getCustomerId().equals(request.getCustomerId())) throw error(HttpStatus.BAD_REQUEST, "QUOTE_INVALID_OPPORTUNITY", "商機不屬於所選客戶。");
            terms.setOpportunity(opportunity);
        }
        terms.setValidUntil(request.getValidUntil()); terms.setPaymentTerms(text(request.getPaymentTerms()));
        terms.setDeliveryTerms(text(request.getDeliveryTerms())); terms.setWarranty(text(request.getWarranty())); terms.setNotes(text(request.getNotes()));
        Map<String, QuoteLine> existing = new HashMap<>();
        for (QuoteLine line : version.getLines()) existing.put(line.getId(), line);
        Set<String> usedIds = new HashSet<>(); List<QuoteLine> next = new ArrayList<>();
        for (QuoteLineRequest line : request.getLines()) {
            QuoteLine old = null;
            if (line.id() != null) {
                if (create || !usedIds.add(line.id()) || !existing.containsKey(line.id()))
                    throw error(HttpStatus.BAD_REQUEST, "QUOTE_INVALID_LINE_ID", "明細 ID 不屬於此版本或重複。");
                old = existing.get(line.id());
            }
            QuoteLine item;
            if (old != null && old.getProduct().getId().equals(line.productId())) item = copyLine(old, true);
            else {
                Product product = products.findById(line.productId()).orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "QUOTE_INVALID_PRODUCT", "產品不存在。"));
                if (!"啟用".equals(product.getStatus())) throw error(HttpStatus.BAD_REQUEST, "QUOTE_INACTIVE_PRODUCT", "停用產品不可新增至報價。");
                item = new QuoteLine(); item.setProduct(product); item.setProductName(product.getName()); item.setSku(product.getSku()); item.setCatalogPrice(product.getPrice());
                if (old != null) item.setId(old.getId());
            }
            item.setQuantity(line.quantity()); item.setUnitPrice(line.unitPrice()); item.setDiscountPercent(line.discountPercent()); item.setTaxPercent(line.taxPercent()); next.add(item);
        }
        version.setTerms(terms); version.getLines().clear(); version.getLines().addAll(next);
        version.setTotals(calculate(next)); version.setApproval(requiresApproval(version) ? ApprovalStatus.Required : ApprovalStatus.NotRequired);
        if (version.getApproval() == ApprovalStatus.NotRequired) version.setRequiresReapproval(false);
        clearReview(version);
    }
    private void clearReview(QuoteVersion version) { version.setApprovalAt(null); version.setApprovalBy(null); version.setApprovalReason(null); }
    private boolean requiresApproval(QuoteVersion version) {
        return version.getTotals().getTotalCents() > 10_000_000L || version.getLines().stream().anyMatch(l -> l.getDiscountPercent().compareTo(BigDecimal.TEN) > 0);
    }
    private QuoteTotals calculate(List<QuoteLine> lines) {
        BigDecimal subtotal = BigDecimal.ZERO, discount = BigDecimal.ZERO, tax = BigDecimal.ZERO;
        for (QuoteLine line : lines) {
            BigDecimal base = line.getUnitPrice().movePointRight(2).multiply(line.getQuantity()).setScale(0, RoundingMode.HALF_UP);
            BigDecimal off = base.multiply(line.getDiscountPercent()).movePointLeft(2).setScale(0, RoundingMode.HALF_UP);
            BigDecimal vat = base.subtract(off).multiply(line.getTaxPercent()).movePointLeft(2).setScale(0, RoundingMode.HALF_UP);
            subtotal = subtotal.add(base); discount = discount.add(off); tax = tax.add(vat);
        }
        BigDecimal total = subtotal.subtract(discount).add(tax);
        if (subtotal.compareTo(MAX_CENTS) > 0 || total.compareTo(MAX_CENTS) > 0)
            throw error(HttpStatus.BAD_REQUEST, "QUOTE_AMOUNT_TOO_LARGE", "報價金額超出前端可精確表示的範圍。");
        QuoteTotals result = new QuoteTotals(); result.setSubtotalCents(subtotal.longValueExact()); result.setDiscountCents(discount.longValueExact());
        result.setTaxCents(tax.longValueExact()); result.setTotalCents(total.longValueExact()); return result;
    }
    private QuoteLine copyLine(QuoteLine source, boolean preserveId) {
        QuoteLine result = new QuoteLine(); BeanUtils.copyProperties(source, result, "id");
        if (preserveId) result.setId(source.getId()); return result;
    }
    private QuoteTerms copyTerms(QuoteTerms source) { QuoteTerms result = new QuoteTerms(); BeanUtils.copyProperties(source, result); return result; }
    private String text(String value) { return value == null ? "" : value; }
    private void reason(boolean accepted, String reason) {
        if (!accepted && (reason == null || reason.isBlank())) throw error(HttpStatus.BAD_REQUEST, "QUOTE_REASON_REQUIRED", "拒絕時必須填寫原因。");
    }
    private void audit(Quote quote, QuoteVersion version, User actor, String action, String detail) {
        QuoteAudit audit = new QuoteAudit(); audit.setAt(clock.instant()); audit.setActorId(actor.getId()); audit.setActorName(actor.getUsername());
        audit.setAction(action); audit.setVersion(version.getVersion()); audit.setDetail(detail); quote.getAudit().add(audit);
    }
}
