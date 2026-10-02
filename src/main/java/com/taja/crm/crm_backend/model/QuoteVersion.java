package com.taja.crm.crm_backend.model;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
@Entity @Table(name = "quote_versions", uniqueConstraints = @UniqueConstraint(columnNames = {"quote_id", "version"}))
@Getter @Setter
public class QuoteVersion {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private String id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "quote_id", nullable = false) private Quote quote;
    @Column(nullable = false) private int version;
    @Column(nullable = false) private long revision = 1;
    @Embedded private QuoteTerms terms = new QuoteTerms();
    @ElementCollection @CollectionTable(name = "quote_lines", joinColumns = @JoinColumn(name = "version_id"))
    @OrderColumn(name = "line_index") private List<QuoteLine> lines = new ArrayList<>();
    @Embedded private QuoteTotals totals = new QuoteTotals();
    @Column(nullable = false) private String currency = "TWD";
    @Enumerated(EnumType.STRING) @Column(nullable = false) private QuoteStatus status = QuoteStatus.Draft;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private ApprovalStatus approval = ApprovalStatus.NotRequired;
    private boolean requiresReapproval;
    @Column(nullable = false) private Instant createdAt;
    @Column(nullable = false) private String createdBy;
    private Instant sentAt;
    private String approvalBy;
    private Instant approvalAt;
    @Column(columnDefinition = "text") private String approvalReason;
    private Instant decisionAt;
    private String decisionBy;
    @Column(columnDefinition = "text") private String decisionReason;
}
