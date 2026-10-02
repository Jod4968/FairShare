package com.fairshare.expense;

import java.io.Serializable;
import java.util.Objects;

public class ExpenseParticipantId implements Serializable {
    private Long expenseId;
    private Long userId;
    public ExpenseParticipantId() {}
    public ExpenseParticipantId(Long expenseId, Long userId) { this.expenseId = expenseId; this.userId = userId; }
    public Long getExpenseId() { return expenseId; }
    public Long getUserId() { return userId; }
    @Override public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof ExpenseParticipantId id)) return false;
        return Objects.equals(expenseId, id.expenseId) && Objects.equals(userId, id.userId);
    }
    @Override public int hashCode() { return Objects.hash(expenseId, userId); }
}
