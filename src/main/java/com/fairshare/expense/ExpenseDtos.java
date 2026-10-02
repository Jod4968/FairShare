package com.fairshare.expense;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.List;

public final class ExpenseDtos {
    private ExpenseDtos() {}
    public record CustomParticipantRequest(@NotNull @Positive Long userId, @NotNull @Positive Long shareMinor) {}
    public record CreateExpenseRequest(
            @NotBlank @Size(max = 240) String description,
            @NotNull @Positive Long amountMinor,
            @NotNull ExpenseCategory category,
            @NotNull SplitType splitType,
            List<Long> participantUserIds,
            List<@Valid CustomParticipantRequest> participants) {}
    public record ParticipantResponse(Long userId, String email, String fullName, Long shareMinor) {}
    public record ExpenseResponse(Long id, Long groupId, Long paidBy, String paidByName,
                                  Long amountMinor, String description, ExpenseCategory category,
                                  SplitType splitType, Instant createdAt, Instant updatedAt,
                                  List<ParticipantResponse> participants) {}
}
