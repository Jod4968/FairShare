package com.fairshare.expense;

import com.fairshare.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "expense_participants")
@IdClass(ExpenseParticipantId.class)
public class ExpenseParticipant {
    @Id @Column(name = "expense_id")
    private Long expenseId;
    @Id @Column(name = "user_id")
    private Long userId;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "expense_id", insertable = false, updatable = false)
    private Expense expense;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;
    @Column(name = "share_minor", nullable = false)
    private Long shareMinor;

    protected ExpenseParticipant() {}
    public ExpenseParticipant(Expense expense, User user, Long shareMinor) {
        this.expense = expense; this.user = user; this.shareMinor = shareMinor;
        this.expenseId = expense.getId(); this.userId = user.getId();
    }
    public Long getExpenseId() { return expenseId; }
    public Long getUserId() { return userId; }
    public User getUser() { return user; }
    public Long getShareMinor() { return shareMinor; }
}
