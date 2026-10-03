package com.fairshare.settlement;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.Instant;

public final class SettlementDtos {
    private SettlementDtos() {}
    public record BalanceResponse(Long userId, String fullName, Long amountPaidMinor, Long amountOwedMinor, Long balanceMinor) {}
    public record SuggestionResponse(Long fromUserId, String fromUserName, Long toUserId, String toUserName, Long amountMinor) {}
    public record CreateSettlementRequest(@NotNull Long toUserId, @NotNull @Positive Long amountMinor) {}
    public record SettlementResponse(Long id, Long fromUserId, String fromUserName, Long toUserId, String toUserName,
                                     Long amountMinor, Instant createdAt, Instant completedAt, String status) {}
}
