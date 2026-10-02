package com.fairshare.expense;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseParticipantRepository extends JpaRepository<ExpenseParticipant, ExpenseParticipantId> {
    List<ExpenseParticipant> findByExpenseIdOrderByUserIdAsc(Long expenseId);
}
