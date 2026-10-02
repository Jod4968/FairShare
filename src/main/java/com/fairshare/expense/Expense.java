package com.fairshare.expense;

import com.fairshare.group.Group;
import com.fairshare.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "expenses")
public class Expense {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "paid_by", nullable = false)
    private User paidBy;
    @Column(name = "amount_minor", nullable = false)
    private Long amountMinor;
    @Column(nullable = false, length = 240)
    private String description;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 32)
    private ExpenseCategory category;
    @Enumerated(EnumType.STRING) @Column(name = "split_type", nullable = false, length = 16)
    private SplitType splitType;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Expense() {}
    public Expense(Group group, User paidBy, Long amountMinor, String description,
                   ExpenseCategory category, SplitType splitType) {
        this.group = group; this.paidBy = paidBy; this.amountMinor = amountMinor;
        this.description = description; this.category = category; this.splitType = splitType;
    }
    @PrePersist void created() { createdAt = updatedAt = Instant.now(); }
    @PreUpdate void updated() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Group getGroup() { return group; }
    public User getPaidBy() { return paidBy; }
    public Long getAmountMinor() { return amountMinor; }
    public String getDescription() { return description; }
    public ExpenseCategory getCategory() { return category; }
    public SplitType getSplitType() { return splitType; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
